package org.commonjava.maven.atlas.spi.neo4j.effective;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.effective.traverse.TraversalType;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.util.logging.Logger;
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
import org.neo4j.tooling.GlobalGraphOperations;

public abstract class AbstractNeo4JEGraphDriver
    implements Runnable, EGraphDriver
{

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
        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
        }
    }

    protected AbstractNeo4JEGraphDriver( final AbstractNeo4JEGraphDriver driver )
    {
        this.graph = driver.graph;
        this.ancestry.addAll( driver.ancestry );
        this.ancestry.add( driver );
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
            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        checkClosed();

        return convertToRelationships( GlobalGraphOperations.at( graph )
                                                            .getAllRelationships() );
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
                final Relationship relationship = from.createRelationshipTo( to, GraphRelType.map( rel.getType() ) );

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

        return new HashSet<ProjectVersionRef>( convertToProjects( GlobalGraphOperations.at( graph )
                                                                                       .getAllNodes() ) );
    }

    public void traverse( final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
        throws GraphDriverException
    {
        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            throw new GraphDriverException( "Project: %s was not found in graph.", root );
        }
        else if ( !inMembership( rootNode ) )
        {
            throw new GraphDriverException( "Project: %s is not in the restricted membership for this graph.", root );
        }

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            TraversalDescription description = Traversal.description()
                                                        .sort( new PathComparator( this ) );

            if ( traversal.getType( i ) == TraversalType.breadth_first )
            {
                description = description.breadthFirst();
            }
            else
            {
                description = description.depthFirst();
            }

            traversal.startTraverse( i, net );
            description = description.evaluator( new MembershipWrappedTraversalEvaluator( this, traversal, i ) );

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
        }
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

    public void restrictProjectMembership( final Set<ProjectVersionRef> refs )
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

    public void restrictRelationshipMembership( final Set<ProjectRelationship<?>> rels )
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

        return nodeMembership.isEmpty() || nodeMembership.contains( node.getId() );
    }

    protected boolean inMembership( final Relationship relationship )
    {
        if ( relationship == null )
        {
            return false;
        }

        return relMembership.isEmpty() || relMembership.contains( relationship.getId() );
    }

    Node getNode( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        final IndexHits<Node> hits = idx.get( Conversions.GAV, ref.toString() );

        return hits.hasNext() ? hits.next() : null;
    }

    Relationship getRelationship( final ProjectRelationship<?> rel )
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
        return ancestry.contains( driver );
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

            return true;
        }

        return false;
    }

    public Set<EProjectCycle> getCycles()
    {
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
        final Node node = getNode( ref );
        if ( node == null )
        {
            return;
        }

        Conversions.setMetadata( key, value, node );
    }

    public void addProjectMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return;
        }

        Conversions.setMetadata( metadata, node );
    }

}