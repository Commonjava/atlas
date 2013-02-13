package org.commonjava.maven.atlas.spi.neo4j.effective;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.filter.AbstractAggregatingFilter;
import org.apache.maven.graph.effective.filter.AbstractTypedFilter;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.AbstractFilteringTraversal;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.effective.traverse.TraversalType;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.apache.maven.graph.spi.effective.GloballyBackedGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.util.logging.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

public abstract class AbstractNeo4JEGraphDriver
    implements Runnable, GloballyBackedGraphDriver, Neo4JEGraphDriver
{

    private final Logger logger = new Logger( getClass() );

    private static final String ALL_RELATIONSHIPS = "all-relationships";

    private static final String ALL_NODES = "all-nodes";

    private static final String UNCONNECTED_NODES = "unconnected-nodes";

    private static final String VARIABLE_NODES = "variable-nodes";

    private static final String ALL_CYCLES = "all-cycles";

    private GraphDatabaseService graph;

    private final Set<Long> nodeMembership = new HashSet<Long>();

    private final Set<Long> relMembership = new HashSet<Long>();

    private final List<AbstractNeo4JEGraphDriver> ancestry = new ArrayList<AbstractNeo4JEGraphDriver>();

    protected AbstractNeo4JEGraphDriver( final GraphDatabaseService graph, final boolean useShutdownHook )
    {
        this.graph = graph;

        printGraphStats();

        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
        }
    }

    private void printGraphStats()
    {
        final Logger logger = new Logger( getClass() );
        logger.info( "Loaded approximately %d nodes.", graph.index()
                                                            .forNodes( ALL_NODES )
                                                            .query( Conversions.GAV, "*" )
                                                            .size() );

        logger.info( "Loaded approximately %d relationships.", graph.index()
                                                                    .forRelationships( ALL_RELATIONSHIPS )
                                                                    .query( Conversions.RELATIONSHIP_ID, "*" )
                                                                    .size() );

        logger.info( "Loaded approximately %d relationship-cycle nodes.", graph.index()
                                                                               .forNodes( ALL_CYCLES )
                                                                               .query( Conversions.CYCLE_ID, "*" )
                                                                               .size() );
    }

    protected AbstractNeo4JEGraphDriver( final AbstractNeo4JEGraphDriver driver )
    {
        this.graph = driver.graph;
        this.ancestry.addAll( driver.ancestry );
        this.ancestry.add( driver );

        new Logger( getClass() ).info( "Ancestry for new driver is:\n  %s", join( ancestry, "\n  " ) );
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final ProjectVersionRef ref )
    {
        checkClosed();

        if ( ref == null )
        {
            return null;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( ALL_NODES );
        final IndexHits<Node> hits = index.get( Conversions.GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            if ( !nodeMembership.isEmpty() && !nodeMembership.contains( node.getId() ) )
            {
                return null;
            }

            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    private void checkClosed()
    {
        if ( graph == null )
        {
            throw new IllegalStateException( "Graph database has been closed!" );
        }
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> index = graph.index()
                                       .forNodes( ALL_NODES );
        final IndexHits<Node> hits = index.get( Conversions.GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            if ( inMembership( node ) )
            {
                final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
                return convertToRelationships( relationships );
            }
        }

        return null;
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        checkClosed();

        printCaller( "GET-ALL-RELATIONSHIPS" );

        Set<ProjectRelationship<?>> rels;
        if ( relMembership.isEmpty() )
        {
            rels =
                new HashSet<ProjectRelationship<?>>( convertToRelationships( graph.index()
                                                                                  .forRelationships( ALL_RELATIONSHIPS )
                                                                                  .query( Conversions.RELATIONSHIP_ID,
                                                                                          "*" ) ) );
        }
        else
        {
            rels = new HashSet<ProjectRelationship<?>>();
            for ( final Long id : relMembership )
            {
                final Relationship r = graph.getRelationshipById( id );
                if ( r != null )
                {
                    final ProjectRelationship<?> rel = Conversions.toProjectRelationship( r );
                    if ( rel != null )
                    {
                        rels.add( rel );
                    }
                }
            }
        }

        return rels;
    }

    public boolean addRelationship( final ProjectRelationship<?> rel )
    {
        checkClosed();

        final Index<Node> index = graph.index()
                                       .forNodes( ALL_NODES );

        final ProjectVersionRef declaring = rel.getDeclaring();
        final ProjectVersionRef target = rel.getTarget()
                                            .asProjectVersionRef();

        final long[] ids = new long[2];
        int i = 0;
        boolean changed = false;
        final Transaction tx = graph.beginTx();
        try
        {
            for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
            {
                final IndexHits<Node> hits = index.get( Conversions.GAV, ref.toString() );
                if ( !hits.hasNext() )
                {
                    changed = true;
                    final Node node = graph.createNode();
                    ids[i] = node.getId();
                    Conversions.toNodeProperties( ref, node );
                    index.add( node, Conversions.GAV, ref.toString() );

                    try
                    {
                        if ( !ref.isRelease() )
                        {
                            graph.index()
                                 .forNodes( VARIABLE_NODES )
                                 .add( node, Conversions.GAV, ref.toString() );
                        }
                    }
                    catch ( final InvalidVersionSpecificationException e )
                    {
                        new Logger( getClass() ).error( "Cannot determine whether project is a release version: %s. Error: %s",
                                                        e, ref, e.getMessage() );
                    }

                    if ( i > 0 )
                    {
                        graph.index()
                             .forNodes( UNCONNECTED_NODES )
                             .add( node, Conversions.GAV, ref.toString() );
                    }
                }
                else
                {
                    ids[i] = hits.next()
                                 .getId();
                }

                i++;
            }

            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            final String relId = Conversions.id( rel );
            final IndexHits<Relationship> relHits = relIdx.get( Conversions.RELATIONSHIP_ID, relId );
            if ( relHits.size() < 1 )
            {
                final Node from = graph.getNodeById( ids[0] );
                final Node to = graph.getNodeById( ids[1] );

                //                if ( ids[0] != ids[1] )
                //                {
                changed = true;
                final Relationship relationship =
                    from.createRelationshipTo( to, GraphRelType.map( rel.getType(), rel.isManaged() ) );

                Conversions.toRelationshipProperties( rel, relationship );
                relIdx.add( relationship, Conversions.RELATIONSHIP_ID, relId );
                //                }

                graph.index()
                     .forNodes( UNCONNECTED_NODES )
                     .remove( from, Conversions.GAV );

                //                final Index<Node> unconnected = graph.index()
                //                                                     .forNodes( UNCONNECTED_NODES );
                //
                //                final IndexHits<Node> unconnHits = unconnected.get( Conversions.GAV, declaring.toString() );
                //
                //                if ( unconnHits.size() > 0 )
                //                {
                //                    changed = true;
                //                    unconnected.remove( from, Conversions.GAV );
                //                }
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return changed;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        checkClosed();

        printCaller( "GET-ALL-PROJECTS" );

        Set<ProjectVersionRef> refs;
        if ( nodeMembership.isEmpty() )
        {
            refs = new HashSet<ProjectVersionRef>( convertToProjects( graph.index()
                                                                           .forNodes( ALL_NODES )
                                                                           .query( Conversions.GAV, "*" ) ) );
        }
        else
        {
            refs = new HashSet<ProjectVersionRef>();
            for ( final Long id : nodeMembership )
            {
                final Node node = graph.getNodeById( id );
                if ( node != null )
                {
                    final ProjectVersionRef ref = Conversions.toProjectVersionRef( node );
                    if ( ref != null )
                    {
                        refs.add( ref );
                    }
                }
            }
        }

        return refs;
    }

    public void traverse( final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
        throws GraphDriverException
    {
        printCaller( "TRAVERSE" );

        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            return;
        }
        else if ( !inMembership( rootNode ) )
        {
            return;
        }

        final Set<GraphRelType> relTypes = getRelTypes( traversal );

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL )
                                                        .sort( new PathComparator( this ) );

            for ( final GraphRelType grt : relTypes )
            {
                description.relationships( grt, Direction.OUTGOING );
            }

            if ( traversal.getType( i ) == TraversalType.breadth_first )
            {
                description = description.breadthFirst();
            }
            else
            {
                description = description.depthFirst();
            }

            traversal.startTraverse( i, net );

            @SuppressWarnings( "rawtypes" )
            final MembershipWrappedTraversalEvaluator checker =
                new MembershipWrappedTraversalEvaluator( this, traversal, i );

            description = description.expand( checker )
                                     .evaluator( checker );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships() );
                if ( rels.isEmpty() )
                {
                    //                    logger.info( "Skipping path with 0 relationships..." );
                    continue;
                }

                //                logger.info( "traversing path with: %d relationships...", rels.size() );
                final ProjectRelationship<?> rel = rels.remove( rels.size() - 1 );

                if ( traversal.traverseEdge( rel, rels, i ) )
                {
                    traversal.edgeTraversed( rel, rels, i );
                }
            }

            traversal.endTraverse( i, net );

            checker.printStats();
        }
    }

    private Set<GraphRelType> getRelTypes( final ProjectNetTraversal traversal )
    {
        final Set<GraphRelType> relTypes = new HashSet<GraphRelType>();
        if ( traversal instanceof AbstractFilteringTraversal )
        {
            final ProjectRelationshipFilter rootFilter = ( (AbstractFilteringTraversal) traversal ).getRootFilter();
            relTypes.addAll( getRelTypes( rootFilter ) );
        }
        else
        {
            relTypes.addAll( Arrays.asList( GraphRelType.values() ) );
        }

        return relTypes;
    }

    private Set<GraphRelType> getRelTypes( final ProjectRelationshipFilter filter )
    {
        final Set<GraphRelType> result = new HashSet<GraphRelType>();

        if ( filter instanceof AbstractTypedFilter )
        {
            final AbstractTypedFilter typedFilter = (AbstractTypedFilter) filter;
            final Set<RelationshipType> types = typedFilter.getRelationshipTypes();
            for ( final RelationshipType rt : types )
            {
                if ( typedFilter.isManagedInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, true );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }

                if ( typedFilter.isConcreteInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, false );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }
            }

            final Set<RelationshipType> dTypes = typedFilter.getDescendantRelationshipTypes();
            for ( final RelationshipType rt : dTypes )
            {
                if ( typedFilter.isManagedInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, true );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }

                if ( typedFilter.isConcreteInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, false );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }
            }
        }
        else if ( filter instanceof AbstractAggregatingFilter )
        {
            final List<? extends ProjectRelationshipFilter> filters =
                ( (AbstractAggregatingFilter) filter ).getFilters();

            for ( final ProjectRelationshipFilter f : filters )
            {
                result.addAll( getRelTypes( f ) );
            }
        }

        return result;
    }

    private void printCaller( final String label )
    {
        final StackTraceElement ste = new Throwable().getStackTrace()[3];

        logger.info( "\n\n\n\n%s called from: %s.%s (%s:%s)\n\n\n\n", label, ste.getClassName(), ste.getMethodName(),
                     ste.getFileName(), ste.getLineNumber() );
    }

    public boolean containsProject( final ProjectVersionRef ref )
    {
        return getNode( ref ) != null;
    }

    public boolean containsRelationship( final ProjectRelationship<?> rel )
    {
        return getRelationship( rel ) != null;
    }

    List<ProjectVersionRef> convertToProjects( final Iterable<Node> nodes )
    {
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
        for ( final Node node : nodes )
        {
            if ( node.getId() == 0 )
            {
                continue;
            }

            if ( !inMembership( node ) )
            {
                continue;
            }

            if ( !Conversions.isType( node, NodeType.PROJECT ) )
            {
                continue;
            }

            refs.add( Conversions.toProjectVersionRef( node ) );
        }

        return refs;
    }

    protected List<ProjectRelationship<?>> convertToRelationships( final Iterable<Relationship> relationships )
    {
        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        for ( final Relationship relationship : relationships )
        {
            if ( !inMembership( relationship ) )
            {
                //                logger.info( "Excluding relationship that's not in the current membership: %d", relationship.getId() );
                continue;
            }

            //            logger.info( "Converting edge: %d to Project relationship.\nDeclaring node id: %d\nTarget node id: %d",
            //                         relationship.getId(), relationship.getStartNode()
            //                                                           .getId(), relationship.getEndNode()
            //                                                                                 .getId() );

            final ProjectRelationship<?> rel = Conversions.toProjectRelationship( relationship );
            if ( rel != null )
            {
                rels.add( rel );
            }
        }

        return rels;
    }

    public void restrictToRoots( final Collection<ProjectVersionRef> roots, final EProjectNet net )
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>( roots );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node node = getNode( ref );
            includeNodeAndRelationships( node );
        }
    }

    private boolean includeNodeAndRelationships( final Node node )
    {
        if ( node == null )
        {
            return false;
        }

        boolean changed = nodeMembership.add( node.getId() );
        if ( !changed )
        {
            return false;
        }

        if ( isMissing( node ) )
        {
            return changed;
        }

        final Iterable<Relationship> rels = node.getRelationships( Direction.OUTGOING );
        if ( rels != null )
        {
            for ( final Relationship r : rels )
            {
                if ( relMembership.add( r.getId() ) )
                {
                    changed = true;
                }
                else
                {
                    continue;
                }

                final Node out = r.getEndNode();
                if ( out != null && out.getId() != node.getId() )
                {
                    changed = includeNodeAndRelationships( out ) || changed;
                }
            }
        }

        return changed;
    }

    public void restrictProjectMembership( final Collection<ProjectVersionRef> refs )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        for ( final ProjectVersionRef ref : refs )
        {
            final IndexHits<Node> hits = idx.get( Conversions.GAV, ref.toString() );
            while ( hits.hasNext() )
            {
                nodeMembership.add( hits.next()
                                        .getId() );
            }
        }
    }

    public void restrictRelationshipMembership( final Collection<ProjectRelationship<?>> rels )
    {
        checkClosed();

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        for ( final ProjectRelationship<?> rel : rels )
        {
            final IndexHits<Relationship> hits = idx.get( Conversions.RELATIONSHIP_ID, Conversions.id( rel ) );
            while ( hits.hasNext() )
            {
                relMembership.add( hits.next()
                                       .getId() );
            }

            refs.add( rel.getDeclaring() );
            refs.add( rel.getTarget()
                         .asProjectVersionRef() );
        }

        restrictProjectMembership( refs );
    }

    protected boolean inMembership( final Node node )
    {
        if ( node == null )
        {
            return false;
        }

        //        logger.info( "Checking membership of: %s against: %s. Result: %s", node, nodeMembership,
        //                     ( nodeMembership.isEmpty() || nodeMembership.contains( node.getId() ) ) );
        return nodeMembership.isEmpty() || nodeMembership.contains( node.getId() );
    }

    protected boolean inMembership( final Relationship relationship )
    {
        if ( relationship == null )
        {
            return false;
        }

        //        logger.info( "Checking membership of: %s against: %s. Result: %s", relationship, relMembership,
        //                     relMembership.isEmpty() || relMembership.contains( relationship.getId() ) );
        return relMembership.isEmpty() || relMembership.contains( relationship.getId() );
    }

    public Node getNode( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        final IndexHits<Node> hits = idx.get( Conversions.GAV, ref.toString() );

        return hits.hasNext() ? hits.next() : null;
    }

    public Relationship getRelationship( final ProjectRelationship<?> rel )
    {
        return getRelationship( Conversions.id( rel ) );
    }

    Relationship getRelationship( final String relId )
    {
        checkClosed();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        final IndexHits<Relationship> hits = idx.get( Conversions.RELATIONSHIP_ID, relId );

        return hits.hasNext() ? hits.next() : null;
    }

    public synchronized void close()
        throws IOException
    {
        if ( ancestry.isEmpty() )
        {
            if ( graph != null )
            {
                try
                {
                    graph.shutdown();
                    graph = null;
                }
                catch ( final Exception e )
                {
                    throw new IOException( "Failed to shutdown: " + e.getMessage(), e );
                }
            }
        }
        else
        {
            // "close" this derivative driver...
            graph = null;
        }
    }

    public void run()
    {
        try
        {
            close();
        }
        catch ( final IOException e )
        {
            new Logger( getClass() ).error( "Failed to shutdown graph database. Reason: %s", e, e.getMessage() );
        }
    }

    public boolean isDerivedFrom( final EGraphDriver driver )
    {
        return driver == this || ancestry.contains( driver );
    }

    private boolean isMissing( final Node node )
    {
        final String gav = Conversions.getStringProperty( Conversions.GAV, node );
        if ( gav == null )
        {
            return true;
        }

        final IndexHits<Node> hits = graph.index()
                                          .forNodes( UNCONNECTED_NODES )
                                          .get( Conversions.GAV, gav );

        return hits.size() > 0;
    }

    public boolean isMissing( final ProjectVersionRef ref )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( UNCONNECTED_NODES )
                                          .get( Conversions.GAV, ref.toString() );

        return hits.size() > 0;
    }

    public boolean hasMissingProjects()
    {
        final Index<Node> index = graph.index()
                                       .forNodes( UNCONNECTED_NODES );

        final IndexHits<Node> hits = index.query( Conversions.GAV, "*" );

        for ( final Node hit : hits )
        {
            new Logger( getClass() ).info( "Found missing project: %s", hit.getProperty( Conversions.GAV ) );
        }

        return hits.size() > 0;
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        final Index<Node> index = graph.index()
                                       .forNodes( UNCONNECTED_NODES );

        final IndexHits<Node> hits = index.query( Conversions.GAV, "*" );
        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        while ( hits.hasNext() )
        {
            final Node node = hits.next();
            if ( !Conversions.isType( node, NodeType.PROJECT ) )
            {
                continue;
            }

            result.add( Conversions.toProjectVersionRef( node ) );
        }

        return result;
    }

    public boolean hasVariableProjects()
    {
        final Index<Node> index = graph.index()
                                       .forNodes( VARIABLE_NODES );

        final IndexHits<Node> hits = index.query( Conversions.GAV, "*" );

        for ( final Node hit : hits )
        {
            new Logger( getClass() ).info( "Found variable project: %s", hit.getProperty( Conversions.GAV ) );
        }

        return hits.size() > 0;
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        final Index<Node> index = graph.index()
                                       .forNodes( VARIABLE_NODES );

        final IndexHits<Node> hits = index.query( Conversions.GAV, "*" );
        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        while ( hits.hasNext() )
        {
            final Node node = hits.next();

            if ( !Conversions.isType( node, NodeType.PROJECT ) )
            {
                continue;
            }

            result.add( Conversions.toProjectVersionRef( node ) );
        }

        return result;
    }

    public boolean addCycle( final EProjectCycle cycle )
    {
        final Set<String> relIds = new HashSet<String>();
        final Set<Relationship> relationships = new HashSet<Relationship>();
        final Set<Long> relationshipIds = new HashSet<Long>();
        for ( final ProjectRelationship<?> rel : cycle )
        {
            relIds.add( Conversions.id( rel ) );
            final Relationship relationship = getRelationship( rel );
            if ( relationship != null )
            {
                relationships.add( relationship );
                relationshipIds.add( relationship.getId() );
            }
        }

        final String rawCycleId = join( relIds, "," );
        final String cycleId = DigestUtils.shaHex( rawCycleId );
        final Index<Node> index = graph.index()
                                       .forNodes( ALL_CYCLES );

        final IndexHits<Node> hits = index.get( Conversions.CYCLE_ID, cycleId );

        if ( hits.size() < 1 )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                final Node node = graph.createNode();
                Conversions.toNodeProperties( cycleId, rawCycleId, cycle.getAllParticipatingProjects(), node );

                index.add( node, Conversions.CYCLE_ID, cycleId );

                if ( !nodeMembership.isEmpty() )
                {
                    nodeMembership.add( node.getId() );
                }

                for ( final Relationship r : relationships )
                {
                    String cycleRefs = Conversions.getMetadata( Conversions.CYCLE_MEMBERSHIP, r );
                    if ( cycleRefs == null )
                    {
                        cycleRefs = "";
                    }

                    if ( !cycleRefs.contains( cycleId ) )
                    {
                        Conversions.setMetadata( Conversions.CYCLE_MEMBERSHIP, cycleRefs + "," + cycleId, r );
                    }
                }

                for ( final ProjectVersionRef ref : cycle.getAllParticipatingProjects() )
                {
                    final Node affectedNode = getNode( ref );
                    final Relationship relationship = affectedNode.createRelationshipTo( node, GraphRelType.CYCLE );
                    if ( !relMembership.isEmpty() )
                    {
                        relMembership.add( relationship.getId() );
                    }
                }

                tx.success();
                return true;
            }
            finally
            {
                tx.finish();
            }
        }

        return false;
    }

    public Set<EProjectCycle> getCycles()
    {
        printCaller( "GET-CYCLES" );

        final IndexHits<Node> query = graph.index()
                                           .forNodes( ALL_CYCLES )
                                           .query( Conversions.CYCLE_ID, "*" );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        for ( final Node cycleNode : query )
        {
            if ( !inMembership( cycleNode ) )
            {
                continue;
            }

            if ( !Conversions.isType( cycleNode, NodeType.CYCLE ) )
            {
                continue;
            }

            final String relIdStr = Conversions.getStringProperty( Conversions.CYCLE_RELATIONSHIPS, cycleNode );
            if ( isEmpty( relIdStr ) )
            {
                continue;
            }

            final String[] relIds = relIdStr.split( "\\s*,\\s*" );
            final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>( relIds.length );
            for ( final String relId : relIds )
            {
                final Relationship r = getRelationship( relId );
                if ( r == null )
                {
                    continue;
                }

                final ProjectRelationship<?> pr = Conversions.toProjectRelationship( r );
                if ( pr == null )
                {
                    continue;
                }

                rels.add( pr );
            }

            cycles.add( new EProjectCycle( rels ) );
        }

        return cycles;
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        final Relationship r = getRelationship( rel );
        if ( r == null )
        {
            return false;
        }

        final String cycleMembership = Conversions.getMetadata( Conversions.CYCLE_MEMBERSHIP, r );
        return cycleMembership != null && cycleMembership.contains( Conversions.id( rel ) );
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return false;
        }

        return node.getSingleRelationship( GraphRelType.CYCLE, Direction.OUTGOING ) != null;
    }

    public void recomputeIncompleteSubgraphs()
    {
        // NOP, handled automatically.
    }

    public Map<String, String> getProjectMetadata( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return null;
        }

        return Conversions.getMetadataMap( node );
    }

    public void addProjectMetadata( final ProjectVersionRef ref, final String key, final String value )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = getNode( ref );
            if ( node == null )
            {
                tx.failure();
                return;
            }

            Conversions.setMetadata( key, value, node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    public void addProjectMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = getNode( ref );
            if ( node == null )
            {
                tx.failure();
                return;
            }

            Conversions.setMetadata( metadata, node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    public boolean includeGraph( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return false;
        }

        return includeNodeAndRelationships( node );
    }

    public ExecutionResult executeFrom( final String cypher, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        return executeFrom( cypher, null, roots );
    }

    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params,
                                        final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        if ( cypher.toLowerCase()
                   .startsWith( "start" ) )
        {
            throw new GraphDriverException(
                                            "Leave off the START clause when supplying ProjectVersionRef instances as query roots:\n'%s'",
                                            cypher );
        }

        final StringBuilder sb = new StringBuilder();
        for ( final ProjectVersionRef root : roots )
        {
            final Node node = getNode( root );
            if ( node != null )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( ", " );
                }
                sb.append( node.getId() );
            }
        }

        if ( sb.length() < 1 )
        {
            sb.append( "*" );
        }

        return execute( String.format( "START n=node(%s) %s", sb, cypher ), params );
    }

    public ExecutionResult executeFrom( final String cypher, final ProjectRelationship<?> rootRel )
        throws GraphDriverException
    {
        return executeFrom( cypher, null, rootRel );
    }

    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params,
                                        final ProjectRelationship<?> rootRel )
        throws GraphDriverException
    {
        if ( cypher.toLowerCase()
                   .startsWith( "start" ) )
        {
            throw new GraphDriverException(
                                            "Leave off the START clause when supplying ProjectRelationship instances as query roots:\n'%s'",
                                            cypher );
        }

        String id = "*";
        if ( rootRel != null )
        {
            final Relationship r = getRelationship( rootRel );
            if ( r != null )
            {
                id = Long.toString( r.getId() );
            }
        }

        return execute( String.format( "START r=relationship(%s) %s", id, cypher ), params );
    }

    public ExecutionResult execute( final String cypher )
        throws GraphDriverException
    {
        return execute( cypher, null );
    }

    public ExecutionResult execute( final String cypher, final Map<String, Object> params )
        throws GraphDriverException
    {
        final ExecutionEngine engine = new ExecutionEngine( graph );
        return params == null ? engine.execute( cypher ) : engine.execute( cypher, params );
    }
}