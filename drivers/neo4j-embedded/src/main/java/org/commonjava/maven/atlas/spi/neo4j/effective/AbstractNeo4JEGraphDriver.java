/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.spi.neo4j.effective;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
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
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.MembershipWrappedTraversalEvaluator;
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

    private static final String ALL_CYCLES = "all-cycles";

    private static final String METADATA_INDEX_PREFIX = "has-metadata-";

    private GraphDatabaseService graph;

    private final Set<Long> roots = new HashSet<Long>();

    private final List<AbstractNeo4JEGraphDriver> ancestry = new ArrayList<AbstractNeo4JEGraphDriver>();

    private boolean useShutdownHook;

    private ProjectRelationshipFilter filter;

    protected AbstractNeo4JEGraphDriver( final AbstractNeo4JEGraphDriver driver,
                                         final ProjectRelationshipFilter filter, final ProjectVersionRef... rootRefs )
        throws GraphDriverException
    {
        //        logger.info( "Creating new graph driver, derived from parent: %s with roots: %s and filter: %s", driver,
        //                     join( rootRefs, ", " ), filter );

        this.filter = filter;
        this.graph = driver.graph;
        this.ancestry.addAll( driver.ancestry );
        this.ancestry.add( driver );

        if ( rootRefs.length > 0 )
        {
            Transaction tx = null;
            try
            {
                tx = graph.beginTx();

                for ( final ProjectVersionRef ref : rootRefs )
                {
                    Node n = getNode( ref );
                    if ( n == null )
                    {
                        n = newProjectNode( ref );
                        //                        logger.info( "Created project node for root: %s with id: %d", ref, n.getId() );
                    }
                    //                    else
                    //                    {
                    //                        logger.info( "Reusing project node for root: %s with id: %d", ref, n.getId() );
                    //                    }

                    roots.add( n.getId() );
                }

                //                logger.info( "Committing graph transaction." );
                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
    }

    protected AbstractNeo4JEGraphDriver( final GraphDatabaseService graph, final boolean useShutdownHook )
    {
        this.graph = graph;
        this.useShutdownHook = useShutdownHook;

        printGraphStats();

        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
        }
    }

    public Set<Long> getRootIds()
    {
        return roots;
    }

    protected boolean isUseShutdownHook()
    {
        return useShutdownHook;
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

        printCaller( "GET-ALL-RELATIONSHIPS" );

        Set<ProjectRelationship<?>> rels = null;
        if ( roots.isEmpty() )
        {
            rels =
                new HashSet<ProjectRelationship<?>>( convertToRelationships( graph.index()
                                                                                  .forRelationships( ALL_RELATIONSHIPS )
                                                                                  .query( Conversions.RELATIONSHIP_ID,
                                                                                          "*" ) ) );
        }
        else
        {
            final String query =
                String.format( "START a=node(%s) MATCH p=(a)-[:%s*]->() RETURN p", join( roots, ", " ),
                               join( GraphRelType.atlasRelationshipTypes(), "|" ) );

            //            logger.info( "Executing cypher query:\n\t%s", query );

            ExecutionResult result;
            try
            {
                rels = new HashSet<ProjectRelationship<?>>();
                result = execute( query );
                if ( result != null )
                {
                    //                    logger.info( "Iterating query result" );
                    final Iterator<Path> pathIt = result.columnAs( "p" );

                    nextResult: while ( pathIt.hasNext() )
                    {
                        final Path p = pathIt.next();
                        final Relationship r = p.lastRelationship();

                        //                        logger.info( "Rel: %s\nPath: %s", r, p );

                        if ( r == null )
                        {
                            //                            logger.info( "Relationship: %s is null. Continuing.", r );
                            continue;
                        }

                        if ( filter != null )
                        {
                            //                            logger.info( "Applying filter: %s", filter );

                            final List<ProjectRelationship<?>> path = convertToRelationships( p.relationships() );
                            ProjectRelationshipFilter f = filter;
                            for ( final ProjectRelationship<?> pr : path )
                            {
                                if ( !f.accept( pr ) )
                                {
                                    continue nextResult;
                                }

                                f = f.getChildFilter( pr );
                            }

                            rels.add( path.get( path.size() - 1 ) );
                        }
                        else
                        {
                            final ProjectRelationship<?> pr = Conversions.toProjectRelationship( r );
                            //                            logger.info( "Adding %s to result.", pr );
                            rels.add( pr );
                        }
                    }

                    //                    logger.info( "Gathered %d results.", rels.size() );
                }
            }
            catch ( final GraphDriverException e )
            {
                logger.error( "Failed to run cypher query to find relationships connected to root(s): %s.\nReason: %s",
                              e, roots, e.getMessage() );
            }
        }

        return rels;
    }

    public boolean addRelationship( final ProjectRelationship<?> rel )
    {
        checkClosed();

        //        logger.info( "Adding relationship: %s", rel );

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
                    final Node node = newProjectNode( ref );
                    //                    logger.info( "Created project node: %s with id: %d", ref, node.getId() );
                    ids[i] = node.getId();
                }
                else
                {
                    ids[i] = hits.next()
                                 .getId();

                    //                    logger.info( "Using existing project node: %s", ids[i] );
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

                //                logger.info( "Creating graph relationship for: %s between node: %d and node: %d", rel, ids[0], ids[1] );

                //                if ( ids[0] != ids[1] )
                //                {
                changed = true;
                final Relationship relationship =
                    from.createRelationshipTo( to, GraphRelType.map( rel.getType(), rel.isManaged() ) );

                //                logger.info( "New relationship: %d has type: %s", relationship.getId(), relationship.getType() );

                Conversions.toRelationshipProperties( rel, relationship );
                relIdx.add( relationship, Conversions.RELATIONSHIP_ID, relId );
                //                }

                Conversions.markConnected( from, true );

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

            //            logger.info( "Committing graph transaction." );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return changed;
    }

    private Node newProjectNode( final ProjectVersionRef ref )
    {
        final Node node = graph.createNode();
        Conversions.toNodeProperties( ref, node, false );

        graph.index()
             .forNodes( ALL_NODES )
             .add( node, Conversions.GAV, ref.toString() );

        return node;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return new HashSet<ProjectVersionRef>( convertToProjects( getAllProjectNodesWhere( false, "has(n.gav)" ) ) );
    }

    private Set<ProjectVersionRef> getAllFlaggedProjects( final String flag, final boolean state )
    {
        final Iterable<Node> hits =
            getAllProjectNodesWhere( true, String.format( "has(n.gav) and n.%s = %s", flag, state ) );
        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        for ( final Node node : hits )
        {
            if ( !Conversions.isType( node, NodeType.PROJECT ) )
            {
                continue;
            }

            result.add( Conversions.toProjectVersionRef( node ) );
        }

        return result;
    }

    private boolean hasFlaggedProject( final String flag, final boolean state )
    {
        final Iterable<Node> hits =
            getAllProjectNodesWhere( true, String.format( "has(n.gav) and n.%s = %s", flag, state ) );

        return hits.iterator()
                   .hasNext();
    }

    private Iterable<Node> getAllProjectNodes()
    {
        return getAllProjectNodesWhere( false, "has(n.gav)" );
    }

    private Iterable<Node> getAllProjectNodesWhere( final boolean existence, final String where )
    {
        checkClosed();

        printCaller( "GET-ALL-PROJECTS (where; existence-only? " + existence + ")" );

        if ( roots.isEmpty() )
        {
            return graph.index()
                        .forNodes( ALL_NODES )
                        .query( Conversions.GAV, "*" );
        }
        else
        {
            final String query =
                String.format( "START a=node(%s) MATCH p=(a)-[:%s]->(n) WHERE %s RETURN n AS node, p AS path %s",
                               join( roots, ", " ), join( GraphRelType.atlasRelationshipTypes(), "|" ), where,
                               ( existence ? "limit 1" : "" ) );

            ExecutionResult result;
            try
            {
                final Set<Node> refs = new HashSet<Node>();
                result = execute( query );
                if ( result != null )
                {
                    nextResult: for ( final Map<String, Object> map : result )
                    {
                        final Node n = (Node) map.get( "node" );
                        if ( n == null )
                        {
                            continue;
                        }

                        if ( filter != null )
                        {
                            final Path p = (Path) map.get( "path" );
                            if ( p == null )
                            {
                                continue nextResult;
                            }

                            final List<ProjectRelationship<?>> path = convertToRelationships( p.relationships() );
                            ProjectRelationshipFilter f = filter;
                            for ( final ProjectRelationship<?> pr : path )
                            {
                                if ( !f.accept( pr ) )
                                {
                                    continue nextResult;
                                }

                                f = f.getChildFilter( pr );
                            }

                            refs.add( n );
                        }
                        else
                        {
                            refs.add( n );
                        }
                    }
                }
            }
            catch ( final GraphDriverException e )
            {
                logger.error( "Failed to run cypher query to find nodes connected to root(s): %s.\nReason: %s", e,
                              roots, e.getMessage() );
            }
        }

        return Collections.emptySet();
    }

    public void traverse( final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
        throws GraphDriverException
    {
        printCaller( "TRAVERSE" );

        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            //            logger.info( "Root node not found! (root: %s)", root );
            return;
        }

        final Set<GraphRelType> relTypes = getRelTypes( traversal );

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            //            logger.info( "PASS: %d", i );

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

            //            logger.info( "starting traverse of: %s", net );
            traversal.startTraverse( i, net );

            @SuppressWarnings( "rawtypes" )
            final MembershipWrappedTraversalEvaluator checker =
                new MembershipWrappedTraversalEvaluator( this, traversal, i );

            description = description.expand( checker )
                                     .evaluator( checker );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                //                logger.info( "traversing: %s", path );
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
        //        final StackTraceElement ste = new Throwable().getStackTrace()[3];
        //
        //        logger.info( "\n\n\n\n%s called from: %s.%s (%s:%s)\n\n\n\n", label, ste.getClassName(), ste.getMethodName(),
        //                     ste.getFileName(), ste.getLineNumber() );
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

            if ( !Conversions.isType( node, NodeType.PROJECT ) )
            {
                continue;
            }

            refs.add( Conversions.toProjectVersionRef( node ) );
        }

        return refs;
    }

    public List<ProjectRelationship<?>> convertToRelationships( final Iterable<Relationship> relationships )
    {
        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        for ( final Relationship relationship : relationships )
        {
            final ProjectRelationship<?> rel = Conversions.toProjectRelationship( relationship );
            if ( rel != null )
            {
                rels.add( rel );
            }
        }

        return rels;
    }

    public Node getNode( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        final IndexHits<Node> hits = idx.get( Conversions.GAV, ref.toString() );

        final Node node = hits.hasNext() ? hits.next() : null;

        //        logger.info( "Query result for node: %s is: %s", ref, node );

        if ( node == null || !hasPathTo( node ) )
        {
            return null;
        }

        return node;
    }

    private boolean hasPathTo( final Node node )
    {
        if ( node == null )
        {
            return false;
        }

        if ( roots == null || roots.isEmpty() )
        {
            return true;
        }

        if ( roots.contains( node.getId() ) )
        {
            return true;
        }

        boolean connected = false;

        final String query =
            String.format( "START a=node(%s), b=node(%d) MATCH p=(a)-[:%s]->(b) RETURN p LIMIT 1", join( roots, ", " ),
                           node.getId(), join( GraphRelType.atlasRelationshipTypes(), "|" ) );

        //        logger.info( "checking path to graph roots with query:\n\t%s", query );

        ExecutionResult result;
        try
        {
            result = execute( query );
            connected = result != null && result.iterator()
                                                .hasNext();
        }
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to run cypher query to determine root connectedness to node: %s (roots: %s).\nReason: %s",
                          e, node, roots, e.getMessage() );
        }

        return connected;
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

    @SuppressWarnings( "unused" )
    private boolean isMissing( final Node node )
    {
        return !Conversions.isConnected( node );
    }

    public boolean isMissing( final ProjectVersionRef ref )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( ALL_NODES )
                                          .get( Conversions.GAV, ref.toString() );

        if ( hits.size() > 0 )
        {
            return !Conversions.isConnected( hits.next() );
        }

        return false;
    }

    public boolean hasMissingProjects()
    {
        return hasFlaggedProject( Conversions.CONNECTED, false );
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        return getAllFlaggedProjects( Conversions.CONNECTED, false );
    }

    public boolean hasUnresolvedVariableProjects()
    {
        final Iterable<Node> hits =
            getAllProjectNodesWhere( true, String.format( "has(n.gav) and n.%s = %s and not(has(n._selected-version))",
                                                          Conversions.VARIABLE, true ) );

        return hits.iterator()
                   .hasNext();

    }

    public Set<ProjectVersionRef> getUnresolvedVariableProjects()
    {
        final Iterable<Node> hits =
            getAllProjectNodesWhere( false,
                                     String.format( "has(n.gav) and n.%s = %s and not(has(n._selected-version))",
                                                    Conversions.VARIABLE, true ) );

        return new HashSet<ProjectVersionRef>( convertToProjects( hits ) );
    }

    public boolean hasVariableProjects()
    {
        return hasFlaggedProject( Conversions.VARIABLE, true );
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        return getAllFlaggedProjects( Conversions.VARIABLE, true );
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
                    affectedNode.createRelationshipTo( node, GraphRelType.CYCLE );
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

        return true;
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
        if ( cypher.startsWith( "START" ) )
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
        if ( cypher.startsWith( "START" ) )
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

    public void reindex()
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Iterable<Node> nodes = getAllProjectNodes();
            for ( final Node node : nodes )
            {
                final String gav = Conversions.getStringProperty( Conversions.GAV, node );
                if ( gav == null )
                {
                    continue;
                }

                final Map<String, String> md = Conversions.getMetadataMap( node );
                if ( md == null || md.isEmpty() )
                {
                    continue;
                }

                for ( final String key : md.keySet() )
                {
                    graph.index()
                         .forNodes( METADATA_INDEX_PREFIX + key )
                         .add( node, Conversions.GAV, gav );
                }
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        final IndexHits<Node> nodes = graph.index()
                                           .forNodes( METADATA_INDEX_PREFIX + key )
                                           .query( Conversions.GAV, "*" );

        return new HashSet<ProjectVersionRef>( convertToProjects( nodes ) );
    }

    public void selectVersionFor( final ProjectVersionRef variable, final ProjectVersionRef select )
        throws GraphDriverException
    {
        if ( !select.isSpecificVersion() )
        {
            throw new GraphDriverException( "Cannot select non-concrete version! Attempted to select: %s", select );
        }

        if ( variable.isSpecificVersion() )
        {
            throw new GraphDriverException(
                                            "Cannot select version if target is already a concrete version! Attempted to select for: %s",
                                            variable );
        }

        // FIXME: Simply adding a resolved version may be a problem here, since it will likely change the subgraph that's associated.
        // FIXME: Perhaps it'd be better to adjust the RELATIONSHIPS and note the original node that was replaced.
        Conversions.selectVersion( getNode( variable ), variable, (SingleVersion) select.getVersionSpec() );
    }

    public Map<ProjectVersionRef, ProjectVersionRef> clearSelectedVersions()
    {
        final Map<ProjectVersionRef, ProjectVersionRef> selected = getSelectedVersions();

        for ( final ProjectVersionRef variable : selected.keySet() )
        {
            // FIXME: Perhaps it'd be better to adjust the RELATIONSHIPS using a note of the original node that had been replaced.
            Conversions.deselectVersion( getNode( variable ), variable );
        }

        return selected;
    }

    public Map<ProjectVersionRef, ProjectVersionRef> getSelectedVersions()
    {
        final Iterable<Node> nodes = getAllProjectNodesWhere( false, "has(n._selected-version)" );
        final Map<ProjectVersionRef, ProjectVersionRef> selected = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        for ( final Node node : nodes )
        {
            final ProjectVersionRef var = Conversions.toProjectVersionRef( node, false );
            final ProjectVersionRef sel = Conversions.toProjectVersionRef( node, true );

            selected.put( var, sel );
        }

        return selected;
    }
}
