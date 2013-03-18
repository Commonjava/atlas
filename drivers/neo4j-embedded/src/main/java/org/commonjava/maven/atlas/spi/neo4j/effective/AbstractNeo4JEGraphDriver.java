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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionSpec;
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
        //        logger.debug( "Creating new graph driver, derived from parent: %s with roots: %s and filter: %s", driver,
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
                    logger.debug( "Looking for existing node for root ref: %s", ref );
                    Node n = getNode( ref );
                    if ( n == null )
                    {
                        n = newProjectNode( ref );
                        logger.debug( "Created project node for root: %s with id: %d", ref, n.getId() );
                    }
                    else
                    {
                        logger.debug( "Reusing project node for root: %s with id: %d", ref, n.getId() );
                    }

                    roots.add( n.getId() );
                }

                //                logger.debug( "Committing graph transaction." );
                tx.success();
            }
            finally
            {
                if ( tx != null )
                {
                    tx.finish();
                }
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
        return convertToRelationships( getAllRelationshipsTargeting( null ) );
    }

    public Iterable<Relationship> getAllRelationshipsTargeting( final String where, final Long... ids )
    {
        final Set<Relationship> rels = new HashSet<Relationship>();
        final Map<Node, Path> result = getResultsTargeting( false, where, ids );
        nextResult: for ( final Path p : result.values() )
        {
            final Relationship r = p.lastRelationship();

            //                        logger.debug( "Rel: %s\nPath: %s", r, p );

            if ( r == null )
            {
                //                            logger.debug( "Relationship: %s is null. Continuing.", r );
                continue;
            }

            if ( filter != null )
            {
                //                            logger.debug( "Applying filter: %s", filter );

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

                rels.add( r );
            }
            else
            {
                rels.add( r );
            }
        }

        return rels;
    }

    private Map<Node, Path> getResultsTargeting( final boolean checkExistence, final String where, final Long... ids )
    {
        checkClosed();

        printCaller( "GET-ALL(where: " + where + ", targeting: " + join( ids, ", " ) + ")" );

        /* @formatter:off */
        final String baseQuery = "START a=node(%s)%s "
                            + "\nMATCH p=(a)-[:%s*]->(n) " 
                            + "\nWHERE "
                            + ( roots == null || roots.isEmpty() ? "" : 
                                String.format(
                                      "\n  none( "
                                    + "\n    r in relationships(p) "
                                    + "\n      WHERE has(r._deselected_for) AND any(x in r._deselected_for WHERE x IN [%s])"
                                    + "\n  )"
                                    + "\n AND ", 
                                join( roots, ", ")
                                )
                              )
                            + "has(n.gav) "
                            + "\n  %s "
                            + "\nRETURN n as node, p as path %s";
        
        final String query = String.format( baseQuery,
                                            ( roots == null || roots.isEmpty() ? "*" : join( roots, ", " ) ),
                                            ( ids.length < 1 ? "" : ", n=node(" + join( ids, ", " ) + ")" ),
                                            join( GraphRelType.atlasRelationshipTypes(), "|" ),
                                            ( where == null ? "" : "AND " + where ),
                                            ( checkExistence ? "LIMIT 1" : "" ) );
        /* @formatter:on */

        //            logger.debug( "Executing cypher query:\n\t%s", query );

        final Map<Node, Path> results = new LinkedHashMap<Node, Path>();
        try
        {
            final ExecutionResult execResult = execute( query );
            if ( execResult != null )
            {
                logger.debug( "Iterating query result" );
                final Iterator<Path> pathIt = execResult.columnAs( "path" );
                final Iterator<Node> nodeIt = execResult.columnAs( "node" );

                nextResult: while ( nodeIt.hasNext() )
                {
                    final Node n = nodeIt.next();
                    final Path p = pathIt.hasNext() ? pathIt.next() : null;

                    if ( !checkExistence && p == null )
                    {
                        continue;
                    }

                    //                        logger.debug( "Rel: %s\nPath: %s", r, p );

                    if ( p != null && filter != null )
                    {
                        //                            logger.debug( "Applying filter: %s", filter );
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
                    }

                    results.put( n, p );

                    if ( checkExistence )
                    {
                        break;
                    }
                }

                logger.debug( "Got %d results.", results.size() );
            }
        }
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to run cypher query to find relationships connected to root(s): %s.\nReason: %s", e,
                          roots, e.getMessage() );
        }

        return results;
    }

    public Set<Path> getAllPathsTargeting( final String where, final Long... ids )
    {
        final Set<Path> paths = new HashSet<Path>();
        final Map<Node, Path> result = getResultsTargeting( false, where, ids );
        nextResult: for ( final Path p : result.values() )
        {
            //                        logger.debug( "Rel: %s\nPath: %s", r, p );

            if ( filter != null )
            {
                //                            logger.debug( "Applying filter: %s", filter );

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
            }

            paths.add( p );
        }

        return paths;
    }

    public boolean addRelationship( final ProjectRelationship<?> rel )
    {
        checkClosed();

        logger.debug( "Adding relationship: %s", rel );

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
                    logger.debug( "Created project node: %s with id: %d", ref, node.getId() );
                    ids[i] = node.getId();
                }
                else
                {
                    ids[i] = hits.next()
                                 .getId();

                    logger.debug( "Using existing project node: %s", ids[i] );
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

                logger.debug( "Creating graph relationship for: %s between node: %d and node: %d", rel, ids[0], ids[1] );

                changed = true;
                final Relationship relationship =
                    from.createRelationshipTo( to, GraphRelType.map( rel.getType(), rel.isManaged() ) );

                //                logger.debug( "New relationship: %d has type: %s", relationship.getId(), relationship.getType() );

                Conversions.toRelationshipProperties( rel, relationship );
                relIdx.add( relationship, Conversions.RELATIONSHIP_ID, relId );

                Conversions.markConnected( from, true );
            }

            //            logger.debug( "Committing graph transaction." );
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
        final String query = String.format( "n.%s%s = %s", flag, ( state ? "!" : "?" ), state );

        final Iterable<Node> hits = getAllProjectNodesWhere( false, query );

        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        for ( final Node node : hits )
        {
            final ProjectVersionRef ref = Conversions.toProjectVersionRef( node );

            logger.debug( "HIT %s (ref: %s)", node, ref );

            result.add( ref );
        }

        return result;
    }

    private boolean hasFlaggedProject( final String flag, final boolean state )
    {
        final Iterable<Node> hits = getAllProjectNodesWhere( true, String.format( "n.%s = %s", flag, state ) );

        return hits.iterator()
                   .hasNext();
    }

    private Iterable<Node> getAllProjectNodes()
    {
        return getAllProjectNodesWhere( false, null );
    }

    private Iterable<Node> getAllProjectNodesWhere( final boolean existence, final String where )
    {
        final Set<Node> refs = new HashSet<Node>();
        final Map<Node, Path> result = getResultsTargeting( existence, where );
        int i = 0;

        //                    logger.debug( "Iterating result: %s", result );
        nextResult: for ( final Map.Entry<Node, Path> entry : result.entrySet() )
        {
            final Node n = entry.getKey();
            final Path p = entry.getValue();

            logger.debug( "Result[%d]: node: %s, path: %s", i, n, p );

            if ( n == null )
            {
                continue;
            }

            if ( filter != null )
            {
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
            }

            refs.add( n );

            logger.debug( "Got %d projects so far.", refs.size() );

            i++;
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
            //            logger.debug( "Root node not found! (root: %s)", root );
            return;
        }

        final Set<GraphRelType> relTypes = getRelTypes( traversal );

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            //            logger.debug( "PASS: %d", i );

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

            //            logger.debug( "starting traverse of: %s", net );
            traversal.startTraverse( i, net );

            @SuppressWarnings( "rawtypes" )
            final MembershipWrappedTraversalEvaluator checker =
                new MembershipWrappedTraversalEvaluator( this, traversal, i );

            description = description.expand( checker )
                                     .evaluator( checker );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                //                logger.debug( "traversing: %s", path );
                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships() );
                if ( rels.isEmpty() )
                {
                    //                    logger.debug( "Skipping path with 0 relationships..." );
                    continue;
                }

                //                logger.debug( "traversing path with: %d relationships...", rels.size() );
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
        //        logger.debug( "\n\n\n\n%s called from:\n\n%s\n\n\n\n", label, join( new Throwable().getStackTrace(), "\n" ) );
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

        logger.debug( "Query result for node: %s is: %s\nChecking for path to root(s): %s", ref, node,
                      join( roots, "|" ) );

        if ( !hasPathTo( node ) )
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

        logger.debug( "Checking for path between roots: %s and target node: %s", join( roots, "," ), node.getId() );
        final Map<Node, Path> result = getResultsTargeting( true, null, node.getId() );
        return !result.isEmpty();
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

    //    public boolean hasUnresolvedVariableProjects()
    //    {
    //        final Iterable<Node> hits =
    //            getAllProjectNodesWhere( true, String.format( "n.%s = %s and not(has(n._selected-version))",
    //                                                          Conversions.VARIABLE, true ) );
    //
    //        return hits.iterator()
    //                   .hasNext();
    //
    //    }
    //
    //    public Set<ProjectVersionRef> getUnresolvedVariableProjects()
    //    {
    //        final Iterable<Node> hits =
    //            getAllProjectNodesWhere( false, String.format( "n.%s = %s and not(has(n._selected-version))",
    //                                                           Conversions.VARIABLE, true ) );
    //
    //        return new HashSet<ProjectVersionRef>( convertToProjects( hits ) );
    //    }

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

        logger.debug( "Running query:\n\n%s\n\n", cypher );

        final String query = cypher.replaceAll( "(\\s)\\s+", "$1" );

        return params == null ? engine.execute( query ) : engine.execute( query, params );
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
        logger.debug( "\n\n\n\nSELECT: %s for: %s\n\n\n\n", select, variable );
        if ( roots == null || roots.isEmpty() )
        {
            throw new GraphDriverException(
                                            "Cannot manage version selections unless current network has one or more root projects." );
        }

        final VersionSpec selected = select.getVersionSpec();
        if ( selected.isSingle() )
        {
            final SingleVersion sv = selected.getSingleVersion();
            if ( !sv.isConcrete() )
            {
                throw new GraphDriverException( "Cannot select non-concrete version! Attempted to select: %s", select );
            }
        }
        else
        {
            throw new GraphDriverException( "Cannot select compound version! Attempted to select: %s", select );
        }

        if ( variable.isRelease() )
        {
            throw new GraphDriverException(
                                            "Cannot select version if target is already a concrete version! Attempted to select for: %s",
                                            variable );
        }

        final Node node = getNode( variable );
        if ( node == null )
        {
            throw new GraphDriverException( "Cannot find node in graph for: %s (unless it's been deselected...)",
                                            variable );
        }

        final Iterable<Path> affected = getAllPathsTargeting( null, node.getId() );
        for ( final Path p : affected )
        {
            final Relationship from = p.lastRelationship();
            final ProjectRelationship<?> rel = Conversions.toProjectRelationship( from );
            final ProjectRelationship<?> sel = rel.selectTarget( (SingleVersion) select.getVersionSpec() );

            selectRelationship( p.startNode(), from, sel );
        }
    }

    private Relationship selectRelationship( final Node root, final Relationship from, final ProjectRelationship<?> toPR )
    {
        Relationship to = null;
        Transaction tx = null;
        try
        {
            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            final String toId = Conversions.id( toPR );

            Node fromNode = from.getStartNode();

            tx = graph.beginTx();

            final IndexHits<Relationship> hits = relIdx.get( Conversions.RELATIONSHIP_ID, toId );
            if ( hits.size() < 1 )
            {
                fromNode = getNode( toPR.getDeclaring() );

                Node toNode = getNode( toPR.getTarget()
                                           .asProjectVersionRef() );

                if ( toNode == null )
                {
                    toNode = newProjectNode( toPR.getTarget()
                                                 .asProjectVersionRef() );
                }

                to = fromNode.createRelationshipTo( toNode, from.getType() );

                Conversions.cloneRelationshipProperties( from, to );
                relIdx.add( to, Conversions.RELATIONSHIP_ID, toId );

                Conversions.markConnected( fromNode, true );
                Conversions.markSelectedFor( to, root );
            }
            else
            {
                to = hits.next();
                fromNode = to.getStartNode();
            }

            Conversions.markDeselectedFor( from, root );

            tx.success();
        }
        finally
        {
            if ( tx != null )
            {
                tx.finish();
            }
        }

        return to;
    }

    public Map<ProjectVersionRef, ProjectVersionRef> clearSelectedVersions()
        throws GraphDriverException
    {
        final Set<SelectionInfo> infos = getSelectionInfo();

        final Map<ProjectVersionRef, ProjectVersionRef> clearedMap = createVariableToSelectedMap( infos );

        Transaction tx = null;
        final Set<Long> deleted = new HashSet<Long>();
        try
        {
            for ( final SelectionInfo info : infos )
            {
                if ( tx == null )
                {
                    tx = graph.beginTx();
                }

                final long srId = info.sr.getId();
                final long vrId = info.vr.getId();
                if ( deleted.contains( srId ) || deleted.contains( vrId ) )
                {
                    logger.info( "Selected- or Variable-relationship already deleted:\n  selected: %s\n    deleted? %s\n  variable: %s\n    deleted? %s.\nContinuing to next mapping.",
                                 info.sr, deleted.contains( srId ), info.vr, deleted.contains( vrId ) );
                    continue;
                }

                if ( Conversions.isCloneFor( info.sr, info.vr ) )
                {
                    deleted.add( srId );
                    info.sr.delete();
                }

                for ( final Long rootId : roots )
                {
                    final Node root = graph.getNodeById( rootId );

                    Conversions.removeSelectionAnnotationsFor( info.vr, root );
                    if ( !deleted.contains( srId ) )
                    {
                        Conversions.removeSelectionAnnotationsFor( info.sr, root );
                    }
                }
            }

            if ( tx != null )
            {
                tx.success();
            }
        }
        finally
        {
            if ( tx != null )
            {
                tx.finish();
            }
        }

        return clearedMap;
    }

    public Map<ProjectVersionRef, ProjectVersionRef> getSelectedVersions()
        throws GraphDriverException
    {
        final Set<SelectionInfo> infos = getSelectionInfo();
        return createVariableToSelectedMap( infos );
    }

    private Map<ProjectVersionRef, ProjectVersionRef> createVariableToSelectedMap( final Set<SelectionInfo> infos )
    {
        final Map<ProjectVersionRef, ProjectVersionRef> result =
            new HashMap<ProjectVersionRef, ProjectVersionRef>( infos.size() );

        for ( final SelectionInfo info : infos )
        {
            result.put( Conversions.toProjectVersionRef( info.v ), Conversions.toProjectVersionRef( info.s ) );
        }

        return result;
    }

    public Set<SelectionInfo> getSelectionInfo()
        throws GraphDriverException
    {
        if ( roots == null || roots.isEmpty() )
        {
            throw new GraphDriverException(
                                            "Cannot manage version selections unless current network has one or more root projects." );
        }

        final String typeStr = join( GraphRelType.atlasRelationshipTypes(), "|" );
        final String rootStr = join( roots, ", " );

        /* @formatter:off */
        final String baseQuery =
            "START a=node(%s) " 
                + "MATCH p1=(a)-[:%s*1..]->(s), " 
                + "  p2=(a)-[:%s*1..]->(v) "
                + "WITH v, s, last(relationships(p1)) as r1, last(relationships(p2)) as r2 "
                + "WHERE v.groupId = s.groupId "
                + "    AND v.artifactId = s.artifactId "
                + "    AND has(r1._selected_for) "
                + "    AND any(x in r1._selected_for "
                + "        WHERE x IN [%s]) "
                + "    AND has(r2._deselected_for) "
                + "    AND any(x in r2._deselected_for "
                + "          WHERE x IN [%s]) "
                + "RETURN r1,r2,v,s";
        /* @formatter:on */

        final String query = String.format( baseQuery, rootStr, typeStr, typeStr, rootStr, rootStr );
        final ExecutionResult result = execute( query );

        final Iterator<Node> varNodes = result.columnAs( "v" );
        final Iterator<Node> selNodes = result.columnAs( "s" );
        final Iterator<Relationship> varRels = result.columnAs( "r1" );
        final Iterator<Relationship> selRels = result.columnAs( "r2" );

        final Set<SelectionInfo> selected = new HashSet<SelectionInfo>();
        while ( varNodes.hasNext() )
        {
            final Node v = varNodes.next();
            final Node s = selNodes.hasNext() ? selNodes.next() : null;
            final Relationship vr = varRels.hasNext() ? varRels.next() : null;
            final Relationship sr = selRels.hasNext() ? selRels.next() : null;

            if ( s == null || vr == null || sr == null )
            {
                logger.error( "Found de-selected: %s with missing selected project, variable relationship, or selected relationship!",
                              ( v.hasProperty( Conversions.GAV ) ? v.getProperty( Conversions.GAV ) + "(" + v.getId()
                                  + ")" : v.getId() ) );
                continue;
            }

            selected.add( new SelectionInfo( v, vr, s, sr ) );
        }

        return selected;
    }

    public class SelectionInfo
    {
        final Node v, s;

        final Relationship vr, sr;

        public SelectionInfo( final Node v, final Relationship vr, final Node s, final Relationship sr )
        {
            this.v = v;
            this.vr = vr;
            this.s = s;
            this.sr = sr;
        }
    }

}
