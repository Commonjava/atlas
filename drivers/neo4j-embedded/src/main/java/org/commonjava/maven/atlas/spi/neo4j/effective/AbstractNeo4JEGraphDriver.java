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

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.CONNECTED;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.CYCLE_ID;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.RELATIONSHIP_ID;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.VARIABLE;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.cloneRelationshipProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.convertToProjects;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.convertToRelationships;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getMetadataMap;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getStringProperty;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.id;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.isCloneFor;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.isConnected;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markConnected;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markCycleInjection;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markDeselectedFor;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markSelectedFor;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.removeSelectionAnnotationsFor;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.setMetadata;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toNodeProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectVersionRef;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toRelationshipProperties;

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

    private static final String GRAPH_ATLAS_TYPES_CLAUSE = join( GraphRelType.atlasRelationshipTypes(), "|" );

    /* @formatter:off */
    private static final String CYPHER_CYCLE_RETRIEVAL = String.format( 
        "CYPHER 1.8 START n=node({roots}) " +
        "\nMATCH p=(n)-[:%s*]->()-[r:%s]->(b) " +
        "\nWITH p, n, r, b, ID(b) as bid " +
        "\nWHERE has(r.%s) " +
        // Paths with a cycle MUST have other occurrences of the terminating node:
        "\n    AND length(filter(x in nodes(p) WHERE ID(x)=bid)) > 1" + 
        "\nRETURN p as path", 
        GRAPH_ATLAS_TYPES_CLAUSE, GRAPH_ATLAS_TYPES_CLAUSE, Conversions.CYCLE_INJECTION
    );
    
    private static final String CYPHER_CYCLE_DETECTION = String.format( 
        "CYPHER 1.8 START t=node({to}), f=node({from}) " +
        "\nMATCH p=(t)-[:%s*]->f " +
        "\nWHERE ID(t) <> ID(f) " +
        "\nRETURN p as path " +
        "\nLIMIT 1",
        GRAPH_ATLAS_TYPES_CLAUSE
    );
    
    private static final String CYPHER_SELECTION_RETRIEVAL = String.format(
        "CYPHER 1.8 START a=node({roots}) " 
            + "\nMATCH p1=(a)-[:%s*1..]->(s), " 
            + "\n    p2=(a)-[:%s*1..]->(v) "
            + "\nWITH v, s, last(relationships(p1)) as r1, last(relationships(p2)) as r2 "
            + "\nWHERE v.groupId = s.groupId "
            + "\n    AND v.artifactId = s.artifactId "
            + "\n    AND has(r1._selected_for) "
            + "\n    AND any(x in r1._selected_for "
            + "\n        WHERE x IN {roots}) "
            + "\n    AND has(r2._deselected_for) "
            + "\n    AND any(x in r2._deselected_for "
            + "\n          WHERE x IN {roots}) "
            + "\nRETURN r1,r2,v,s",
        GRAPH_ATLAS_TYPES_CLAUSE, GRAPH_ATLAS_TYPES_CLAUSE
    );
    
    private static final String CYPHER_RETRIEVE_ALL_META = String.format( 
        "CYPHER 1.8 START a=node({roots})%s "
                    + "\nMATCH p=(a)-[:%s*]->(n) " 
                    + "\nWHERE "
                    + "\n    %s "
                    + "\n    has(n.gav) "
                    + "\n    %s "
                    + "\nRETURN n as node, p as path",
        "%s", GRAPH_ATLAS_TYPES_CLAUSE, "%s", "%s"
    );
    
    private static final String CYPHER_ROOTED_CLAUSE = 
        "none(r in relationships(p) "
       + "\n        WHERE has(r._deselected_for) AND any(x in r._deselected_for WHERE x IN {roots}) "
       + "\n    ) "
       + "\n    AND ";


    /* @formatter:on */

    private GraphDatabaseService graph;

    private final Set<Long> roots = new HashSet<Long>();

    private final List<AbstractNeo4JEGraphDriver> ancestry = new ArrayList<AbstractNeo4JEGraphDriver>();

    private boolean useShutdownHook;

    private ProjectRelationshipFilter filter;

    private ExecutionEngine queryEngine;

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

    protected GraphDatabaseService getGraph()
    {
        return graph;
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
                                                            .query( GAV, "*" )
                                                            .size() );

        logger.info( "Loaded approximately %d relationships.", graph.index()
                                                                    .forRelationships( ALL_RELATIONSHIPS )
                                                                    .query( RELATIONSHIP_ID, "*" )
                                                                    .size() );

        logger.info( "Loaded approximately %d relationship-cycle nodes.", graph.index()
                                                                               .forNodes( ALL_CYCLES )
                                                                               .query( CYCLE_ID, "*" )
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
        final IndexHits<Node> hits = index.get( GAV, ref.toString() );

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
        final IndexHits<Node> hits = index.get( GAV, ref.toString() );

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
        final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();
        final Set<Path> result = getAllPathsTargeting( false );
        for ( final Path p : result )
        {
            final List<ProjectRelationship<?>> path = convertToRelationships( p.relationships() );
            logger.debug( "Path: %s", p );

            logger.debug( "Adding %d relationships: %s", path.size(), path );
            rels.addAll( path );
        }

        for ( final ProjectRelationship<?> rel : new HashSet<ProjectRelationship<?>>( rels ) )
        {
            logger.debug( "Checking for self-referential parent: %s", rel );
            if ( rel.getType() == RelationshipType.PARENT && rel.getDeclaring()
                                                                .equals( rel.getTarget()
                                                                            .asProjectVersionRef() ) )
            {
                logger.debug( "Removing self-referential parent: %s", rel );
                rels.remove( rel );
            }
        }

        logger.debug( "returning %d relationships: %s", rels.size(), rels );
        return rels;
    }

    public Set<Relationship> getAllRelationshipsTargeting( final Long... ids )
    {
        return getAllRelationshipsTargeting( null, null, ids );
    }

    public Set<Relationship> getAllRelationshipsTargeting( final String where, final Map<String, Object> whereParams,
                                                           final Long... ids )
    {
        final Set<Relationship> rels = new HashSet<Relationship>();
        final Set<Path> result = getAllPathsTargeting( false, where, whereParams, ids );
        for ( final Path p : result )
        {
            final Relationship r = p.lastRelationship();

            logger.debug( "Rel: %s\nPath: %s", r, p );

            if ( r == null )
            {
                //                            logger.debug( "Relationship: %s is null. Continuing.", r );
                continue;
            }

            rels.add( r );
        }

        return rels;
    }

    private Map<Node, Set<Path>> getResultsTargeting( final String where, final Map<String, Object> whereParams,
                                                      final Long... ids )
    {
        checkClosed();

        printCaller( "GET-ALL(where: " + where + ", targeting: " + join( ids, ", " ) + ")" );

        /* @formatter:off */
        final String query = String.format( CYPHER_RETRIEVE_ALL_META, 
                                            ( ids.length < 1 ? "" : ", n=node({targets})" ), 
                                            ( roots == null || roots.isEmpty() ? "" : CYPHER_ROOTED_CLAUSE ), 
                                            ( where == null ? "" : "AND " + where ) );
        /* @formatter:on */

        final Map<String, Object> params = new HashMap<String, Object>();
        if ( whereParams != null )
        {
            params.putAll( whereParams );
        }

        params.put( "roots", ( roots == null || roots.isEmpty() ? "*" : roots ) );

        if ( ids.length > 0 )
        {
            params.put( "targets", Arrays.asList( ids ) );
        }

        //            logger.debug( "Executing cypher query:\n\t%s", query );

        final Map<Node, Set<Path>> results = new LinkedHashMap<Node, Set<Path>>();
        final ExecutionResult execResult = execute( query, params );
        if ( execResult != null )
        {
            logger.debug( "Iterating query result" );
            final Iterator<Map<String, Object>> mapIt = execResult.iterator();
            //                final Iterator<Path> pathIt = execResult.columnAs( "path" );
            //                final Iterator<Node> nodeIt = execResult.columnAs( "node" );

            nextResult: while ( mapIt.hasNext() )
            {
                final Map<String, Object> map = mapIt.next();
                logger.debug( "Current result row:\n  %s", map );

                final Node n = (Node) map.get( "node" );
                final Path p = (Path) map.get( "path" );

                if ( p == null )
                {
                    continue;
                }

                logger.debug( "Node: %s\nPath: %s", n, p );

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

                Set<Path> paths = results.get( n );
                if ( paths == null )
                {
                    paths = new HashSet<Path>();
                    results.put( n, paths );
                }

                paths.add( p );
            }

            //                logger.debug( "Got %d results.", results.size() );
        }

        //        logger.debug( "Total: %d results:\n\n  %s", results.size(), results );

        return results;
    }

    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final ProjectVersionRef ref )
    {
        final Node n = getNode( ref );
        if ( n == null )
        {
            return null;
        }

        final Set<Path> paths = getAllPathsTargeting( false, new Long[] { n.getId() } );
        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        for ( final Path path : paths )
        {
            result.add( convertToRelationships( path.relationships() ) );
        }

        return result;
    }

    public Set<Path> getAllPathsTargeting( final boolean checkExistence, final Long... ids )
    {
        return getAllPathsTargeting( checkExistence, null, null, ids );
    }

    public Set<Path> getAllPathsTargeting( final boolean checkExistence, final String where,
                                           final Map<String, Object> whereParams, final Long... ids )
    {
        final Set<Path> paths = new HashSet<Path>();

        final Map<Node, Set<Path>> result = getResultsTargeting( where, whereParams, ids );
        for ( final Set<Path> nodePaths : result.values() )
        {
            nextResult: for ( final Path p : nodePaths )
            {
                if ( filter != null )
                {
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

                if ( checkExistence )
                {
                    return paths;
                }
            }
        }

        return paths;
    }

    public Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rels )
    {
        checkClosed();

        final Transaction tx = graph.beginTx();
        final Set<ProjectRelationship<?>> skipped = new HashSet<ProjectRelationship<?>>();
        try
        {
            for ( final ProjectRelationship<?> rel : rels )
            {
                logger.debug( "Adding relationship: %s", rel );

                final Index<Node> index = graph.index()
                                               .forNodes( ALL_NODES );

                final ProjectVersionRef declaring = rel.getDeclaring();
                final ProjectVersionRef target = rel.getTarget()
                                                    .asProjectVersionRef();

                final long[] ids = new long[2];
                int i = 0;
                for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
                {
                    final IndexHits<Node> hits = index.get( GAV, ref.toString() );
                    if ( !hits.hasNext() )
                    {
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

                final String relId = id( rel );
                final IndexHits<Relationship> relHits = relIdx.get( RELATIONSHIP_ID, relId );
                if ( relHits.size() < 1 )
                {
                    final Node from = graph.getNodeById( ids[0] );
                    final Node to = graph.getNodeById( ids[1] );

                    logger.debug( "Creating graph relationship for: %s between node: %d and node: %d", rel, ids[0],
                                  ids[1] );

                    final Relationship relationship =
                        from.createRelationshipTo( to, GraphRelType.map( rel.getType(), rel.isManaged() ) );

                    logger.debug( "New relationship is: %s", relationship );

                    toRelationshipProperties( rel, relationship );
                    relIdx.add( relationship, RELATIONSHIP_ID, relId );

                    markConnected( from, true );

                    if ( introducesCycle( rel, relationship ) )
                    {
                        markCycleInjection( relationship );
                        skipped.add( rel );
                        logger.warn( "%s introduces a cycle!", rel );
                    }
                }
                else
                {
                    // Not SKIPPED per se...just already logged.
                    //                    skipped.add( rel );
                }
            }

            //            logger.debug( "Committing graph transaction." );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return skipped;
    }

    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        final Relationship r = getRelationship( rel );
        return r != null && introducesCycle( rel, r );
    }

    private boolean introducesCycle( final ProjectRelationship<?> rel, final Relationship relationship )
    {
        final Node from = relationship.getStartNode();
        final Node to = relationship.getEndNode();

        if ( from == null || to == null )
        {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "to", to.getId() );
        params.put( "from", from.getId() );

        final ExecutionResult result = execute( CYPHER_CYCLE_DETECTION, params );
        if ( result.iterator()
                   .hasNext() )
        {
            return true;
        }

        return false;
    }

    private Node newProjectNode( final ProjectVersionRef ref )
    {
        final Node node = graph.createNode();
        toNodeProperties( ref, node, false );

        graph.index()
             .forNodes( ALL_NODES )
             .add( node, GAV, ref.toString() );

        return node;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return new HashSet<ProjectVersionRef>( convertToProjects( getAllProjectNodesWhere( false, null, null ) ) );
    }

    private Set<ProjectVersionRef> getAllFlaggedProjects( final String flag, final boolean state )
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "value", state );

        final String query = String.format( "n.%s%s = {value}", flag, ( state ? "!" : "?" ) );

        final Iterable<Node> hits = getAllProjectNodesWhere( false, query, params );

        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        for ( final Node node : hits )
        {
            final ProjectVersionRef ref = toProjectVersionRef( node );

            logger.debug( "HIT %s (ref: %s)", node, ref );

            result.add( ref );
        }

        return result;
    }

    private boolean hasFlaggedProject( final String flag, final boolean state )
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "value", state );

        final Set<Node> hits =
            getAllProjectNodesWhere( true, String.format( "n.%s%s = {value}", flag, ( state ? "!" : "?" ) ), params );

        return !hits.isEmpty();
    }

    private Set<Node> getAllProjectNodes()
    {
        return getAllProjectNodesWhere( false, null, null );
    }

    private Set<Node> getAllProjectNodesWhere( final boolean existence, final String where,
                                               final Map<String, Object> whereParams )
    {
        final Set<Node> refs = new HashSet<Node>();
        final Map<Node, Set<Path>> result = getResultsTargeting( where, whereParams );
        //        int i = 0;

        //                    logger.debug( "Iterating result: %s", result );
        for ( final Map.Entry<Node, Set<Path>> entry : result.entrySet() )
        {
            final Node n = entry.getKey();
            final Set<Path> nodePaths = entry.getValue();

            //            logger.debug( "Result[%d]: node: %s, path: %s", i, n, p );

            if ( n == null )
            {
                continue;
            }

            boolean add = true;
            if ( filter != null )
            {
                add = false;
                nextPath: for ( final Path p : nodePaths )
                {
                    if ( p == null )
                    {
                        continue;
                    }

                    final List<ProjectRelationship<?>> path = convertToRelationships( p.relationships() );
                    ProjectRelationshipFilter f = filter;
                    for ( final ProjectRelationship<?> pr : path )
                    {
                        if ( !f.accept( pr ) )
                        {
                            continue nextPath;
                        }

                        f = f.getChildFilter( pr );
                    }

                    add = true;
                    break;
                }
            }

            if ( add )
            {
                refs.add( n );
            }

            logger.debug( "Got %d projects so far.", refs.size() );

            //            i++;
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
                                                        .sort( new PathComparator() );

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

    public Node getNode( final ProjectVersionRef ref )
    {
        return getNode( ref, true );
    }

    public Node getNode( final ProjectVersionRef ref, final boolean inCurrentGraph )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        final IndexHits<Node> hits = idx.get( GAV, ref.toString() );

        final Node node = hits.hasNext() ? hits.next() : null;

        logger.debug( "Query result for node: %s is: %s\nChecking for path to root(s): %s", ref, node,
                      join( roots, "|" ) );

        if ( inCurrentGraph && !hasPathTo( node ) )
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
        final Set<Path> result = getAllPathsTargeting( true, node.getId() );
        return !result.isEmpty();
    }

    public Relationship getRelationship( final ProjectRelationship<?> rel )
    {
        return getRelationship( id( rel ) );
    }

    Relationship getRelationship( final String relId )
    {
        checkClosed();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        final IndexHits<Relationship> hits = idx.get( RELATIONSHIP_ID, relId );

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
        return !isConnected( node );
    }

    public boolean isMissing( final ProjectVersionRef ref )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( ALL_NODES )
                                          .get( GAV, ref.toString() );

        if ( hits.size() > 0 )
        {
            return !isConnected( hits.next() );
        }

        return false;
    }

    public boolean hasMissingProjects()
    {
        return hasFlaggedProject( CONNECTED, false );
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        return getAllFlaggedProjects( CONNECTED, false );
    }

    //    public boolean hasUnresolvedVariableProjects()
    //    {
    //        final Iterable<Node> hits =
    //            getAllProjectNodesWhere( true, String.format( "n.%s = %s and not(has(n._selected-version))",
    //                                                          VARIABLE, true ) );
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
    //                                                           VARIABLE, true ) );
    //
    //        return new HashSet<ProjectVersionRef>( convertToProjects( hits ) );
    //    }

    public boolean hasVariableProjects()
    {
        return hasFlaggedProject( VARIABLE, true );
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        return getAllFlaggedProjects( VARIABLE, true );
    }

    public boolean addCycle( final EProjectCycle cycle )
    {
        // NOP, auto-detected.
        return false;
    }

    public Set<EProjectCycle> getCycles()
    {
        printCaller( "GET-CYCLES" );

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "roots", ( roots == null || roots.isEmpty() ? "*" : roots ) );

        final ExecutionResult result = execute( CYPHER_CYCLE_RETRIEVAL, params );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        nextPath: for ( final Map<String, Object> record : result )
        {
            final Path p = (Path) record.get( "path" );
            final Node terminus = p.lastRelationship()
                                   .getEndNode();

            final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
            boolean logging = false;

            ProjectRelationshipFilter f = filter;
            for ( final Relationship r : p.relationships() )
            {
                if ( r.getStartNode()
                      .equals( terminus ) )
                {
                    logging = true;
                }

                if ( logging )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r );
                    if ( f != null )
                    {
                        if ( !f.accept( rel ) )
                        {
                            continue nextPath;
                        }
                        else
                        {
                            f = f.getChildFilter( rel );
                        }
                    }

                    rels.add( rel );
                }
            }

            if ( !rels.isEmpty() )
            {
                cycles.add( new EProjectCycle( rels ) );
            }
        }

        return cycles;
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : getCycles() )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : getCycles() )
        {
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
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

        return getMetadataMap( node );
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

            setMetadata( key, value, node );
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

            setMetadata( metadata, node );
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
    {
        return execute( cypher, null );
    }

    public ExecutionResult execute( final String cypher, final Map<String, Object> params )
    {
        checkExecutionEngine();

        logger.debug( "Running query:\n\n%s\n\nWith params:\n\n%s\n\n", cypher, params );

        final String query = cypher.replaceAll( "(\\s)\\s+", "$1" );

        final ExecutionResult result =
            params == null ? queryEngine.execute( query ) : queryEngine.execute( query, params );

        //        logger.info( "Execution plan:\n%s", result.executionPlanDescription() );

        return result;
    }

    private synchronized void checkExecutionEngine()
    {
        if ( queryEngine == null )
        {
            queryEngine = new ExecutionEngine( graph );
        }
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
                final String gav = getStringProperty( GAV, node );
                if ( gav == null )
                {
                    continue;
                }

                final Map<String, String> md = getMetadataMap( node );
                if ( md == null || md.isEmpty() )
                {
                    continue;
                }

                for ( final String key : md.keySet() )
                {
                    graph.index()
                         .forNodes( METADATA_INDEX_PREFIX + key )
                         .add( node, GAV, gav );
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
                                           .query( GAV, "*" );

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

        final Iterable<Path> affected = getAllPathsTargeting( false, node.getId() );
        for ( final Path p : affected )
        {
            final Relationship from = p.lastRelationship();
            final ProjectRelationship<?> rel = toProjectRelationship( from );
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

            final String toId = id( toPR );

            Node fromNode = from.getStartNode();

            tx = graph.beginTx();

            final IndexHits<Relationship> hits = relIdx.get( RELATIONSHIP_ID, toId );
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

                cloneRelationshipProperties( from, to );
                relIdx.add( to, RELATIONSHIP_ID, toId );

                markConnected( fromNode, true );
                markSelectedFor( to, root );
            }
            else
            {
                to = hits.next();
                fromNode = to.getStartNode();
            }

            markDeselectedFor( from, root );

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
                logger.debug( "Clearing selection:\nSelected: %s\nVariable: %s",
                              info.sr.getEndNode()
                                     .getProperty( Conversions.GAV ), info.vr.getEndNode()
                                                                             .getProperty( Conversions.GAV ) );

                if ( tx == null )
                {
                    tx = graph.beginTx();
                }

                final long srId = info.sr.getId();
                final long vrId = info.vr.getId();
                if ( deleted.contains( srId ) || deleted.contains( vrId ) )
                {
                    logger.debug( "Selected- or Variable-relationship already deleted:\n  selected: %s\n    deleted? %s\n  variable: %s\n    deleted? %s.\nContinuing to next mapping.",
                                  info.sr, deleted.contains( srId ), info.vr, deleted.contains( vrId ) );
                    continue;
                }

                if ( isCloneFor( info.sr, info.vr ) )
                {
                    logger.debug( "Deleting cloned relationship from previous selection operation: %s", info.sr );
                    deleted.add( srId );
                    info.sr.delete();
                }

                for ( final Long rootId : roots )
                {
                    final Node root = graph.getNodeById( rootId );

                    removeSelectionAnnotationsFor( info.vr, root );
                    if ( !deleted.contains( srId ) )
                    {
                        logger.debug( "Removing selection annotations for previously selected: %s", info.sr );
                        markDeselectedFor( info.sr, root );
                        //                        removeSelectionAnnotationsFor( info.sr, root );
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
            result.put( toProjectVersionRef( info.v ), toProjectVersionRef( info.s ) );
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

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "roots", roots );

        final ExecutionResult result = execute( CYPHER_SELECTION_RETRIEVAL, params );

        final Iterator<Map<String, Object>> mapIt = result.iterator();

        final Set<SelectionInfo> selected = new HashSet<SelectionInfo>();
        while ( mapIt.hasNext() )
        {
            final Map<String, Object> record = mapIt.next();
            final Node v = (Node) record.get( "v" );
            final Node s = (Node) record.get( "s" );
            final Relationship sr = (Relationship) record.get( "r1" );
            final Relationship vr = (Relationship) record.get( "r2" );

            if ( s == null || vr == null || sr == null )
            {
                logger.error( "Found de-selected: %s with missing selected project, variable relationship, or selected relationship!",
                              ( v.hasProperty( GAV ) ? v.getProperty( GAV ) + "(" + v.getId() + ")" : v.getId() ) );
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
