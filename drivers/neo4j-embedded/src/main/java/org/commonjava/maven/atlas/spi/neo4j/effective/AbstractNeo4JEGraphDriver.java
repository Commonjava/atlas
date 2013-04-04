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
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.RELATIONSHIP_ID;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.cloneRelationshipProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.convertToProjects;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.convertToRelationships;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getInjectedCycles;
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
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectedSet;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toRelationshipProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toSet;

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
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.AtlasCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.ConnectingPathsCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.EndGAVsPathsCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.EndNodesCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.MembershipWrappedTraversalEvaluator;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.RootedNodesCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.RootedRelationshipsCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.SelectionFinderAtlasCollector;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.spi.neo4j.io.NodeIdProjector;
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

    private static final String ALL_RELATIONSHIPS = "all_relationships";

    private static final String ALL_NODES = "all_nodes";

    private static final String CYCLE_INJECTION_IDX = "cycle_injections";

    private static final String VARIABLE_NODES_IDX = "variable_nodes";

    private static final String MISSING_NODES_IDX = "missing_nodes";

    private static final String METADATA_INDEX_PREFIX = "has_metadata_";

    //    private static final String GRAPH_ATLAS_TYPES_CLAUSE = join( GraphRelType.atlasRelationshipTypes(), "|" );

    /* @formatter:off */
//    private static final String CYPHER_SELECTION_RETRIEVAL = String.format(
//        "CYPHER 1.8 START a=node({roots}) " 
//            + "\nMATCH p1=(a)-[:%s*1..]->(s), " 
//            + "\n    p2=(a)-[:%s*1..]->(v) "
//            + "\nWITH v, s, last(relationships(p1)) as r1, last(relationships(p2)) as r2 "
//            + "\nWHERE v.%s = s.%s "
//            + "\n    AND v.%s = s.%s "
//            + "\n    AND has(r1.%s) "
//            + "\n    AND any(x in r1.%s "
//            + "\n        WHERE x IN {roots}) "
//            + "\n    AND has(r2.%s) "
//            + "\n    AND any(x in r2.%s "
//            + "\n          WHERE x IN {roots}) "
//            + "\nRETURN r1,r2,v,s",
//        GRAPH_ATLAS_TYPES_CLAUSE, GRAPH_ATLAS_TYPES_CLAUSE, 
//        Conversions.GROUP_ID, Conversions.GROUP_ID, 
//        Conversions.ARTIFACT_ID, Conversions.ARTIFACT_ID, 
//        Conversions.SELECTED_FOR, Conversions.SELECTED_FOR, 
//        Conversions.DESELECTED_FOR, Conversions.DESELECTED_FOR
//    );
    
    /* @formatter:on */

    private GraphDatabaseService graph;

    private final Set<Node> roots = new HashSet<Node>();

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

                    roots.add( n );
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
        return roots == null ? null : toProjectedSet( roots, new NodeIdProjector() );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();

        for ( final Node n : roots )
        {
            if ( n != null )
            {
                refs.add( toProjectVersionRef( n ) );
            }
        }

        return refs;
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
        if ( roots != null && !roots.isEmpty() )
        {
            final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();

            final RootedRelationshipsCollector checker = new RootedRelationshipsCollector( roots, filter, false );
            collectAtlasRelationships( checker, roots );

            for ( final Relationship r : checker )
            {
                rels.add( toProjectRelationship( r ) );
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
        else
        {
            final IndexHits<Relationship> hits = graph.index()
                                                      .forRelationships( ALL_RELATIONSHIPS )
                                                      .query( RELATIONSHIP_ID, "*" );
            return convertToRelationships( hits );
        }
    }

    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final ProjectVersionRef ref )
    {
        // NOTE: using global lookup here to avoid checking for paths, which we're going to collect below.
        final Node n = getNode( ref, false );
        if ( n == null )
        {
            return null;
        }

        final ConnectingPathsCollector checker =
            new ConnectingPathsCollector( roots, Collections.singleton( n ), filter, false );

        collectAtlasRelationships( checker, roots );

        final Set<Path> paths = checker.getFoundPaths();
        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        for ( final Path path : paths )
        {
            result.add( convertToRelationships( path.relationships() ) );
        }

        return result;
    }

    public Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rels )
    {
        checkClosed();

        Transaction tx = graph.beginTx();
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

                    if ( ids[0] != ids[1] )
                    {
                        final Node to = graph.getNodeById( ids[1] );

                        logger.debug( "Creating graph relationship for: %s between node: %d and node: %d", rel, ids[0],
                                      ids[1] );

                        final Relationship relationship =
                            from.createRelationshipTo( to, GraphRelType.map( rel.getType(), rel.isManaged() ) );

                        logger.debug( "New relationship is: %s", relationship );

                        toRelationshipProperties( rel, relationship );
                        relIdx.add( relationship, RELATIONSHIP_ID, relId );
                    }

                    graph.index()
                         .forNodes( MISSING_NODES_IDX )
                         .remove( from );
                    markConnected( from, true );
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

        tx = graph.beginTx();
        try
        {
            for ( final ProjectRelationship<?> rel : rels )
            {
                if ( skipped.contains( rel ) )
                {
                    continue;
                }

                final Relationship r = getRelationship( rel );
                if ( r == null || markCycle( rel, r ) )
                {
                    skipped.add( rel );
                }
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return skipped;
    }

    public boolean markCycle( final ProjectRelationship<?> rel, final Relationship relationship )
    {
        //        if ( roots == null || roots.isEmpty() )
        //        {
        //            logger.info( "NOT marking cycles for global graph, where there are no root nodes." );
        //            return false;
        //        }

        final Set<Path> cycles = getIntroducedCycles( rel );
        if ( cycles != null && !cycles.isEmpty() )
        {
            markCycleInjection( relationship, cycles );

            final String relId = id( rel );
            graph.index()
                 .forRelationships( CYCLE_INJECTION_IDX )
                 .add( relationship, RELATIONSHIP_ID, relId );

            return true;
        }

        return false;
    }

    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return !getIntroducedCycles( rel ).isEmpty();
    }

    private Set<Path> getIntroducedCycles( final ProjectRelationship<?> rel )
    {
        //        logger.info( "\n\n\n\nCHECKING FOR CYCLES INTRODUCED BY: %s\n\n\n\n", rel );

        final Node from = getNode( rel.getDeclaring() );
        final Node to = getNode( rel.getTarget()
                                    .asProjectVersionRef() );
        if ( from == null || to == null )
        {
            return Collections.emptySet();
        }

        final ConnectingPathsCollector checker = new ConnectingPathsCollector( to, from, filter, false );
        collectAtlasRelationships( checker, Collections.singleton( to ) );

        return checker.getFoundPaths();
        //
        //
        //        final Map<String, Object> params = new HashMap<String, Object>();
        //        params.put( "to", to.getId() );
        //        //        params.put( "from", from.getId() );
        //
        //        //        ExecutionResult result = execute( CYPHER_CYCLE_DETECTION_EXISTING, params );
        //        //        for ( final Map<String, Object> record : result )
        //        //        {
        //        //            final Path p = (Path) record.get( "path" );
        //        //            final Set<Long> cycle = new HashSet<Long>();
        //        //            for ( final Relationship r : p.relationships() )
        //        //            {
        //        //                cycle.add( r.getId() );
        //        //            }
        //        //
        //        //            logger.warn( "\n\n\n\nCYCLE DETECTED!\n\nCycle: %s\n\n\n\n", convertToRelationships( p.relationships() ) );
        //        //            cycles.add( cycle );
        //        //        }
        //
        //        params.put( "from", from.getId() );
        //
        //        final ExecutionResult result = execute( CYPHER_CYCLE_DETECTION_NEW, params );
        //        for ( final Map<String, Object> record : result )
        //        {
        //            final Path p = (Path) record.get( "path" );
        //            final Set<Long> cycle = new LinkedHashSet<Long>();
        //            for ( final Relationship r : p.relationships() )
        //            {
        //                cycle.add( r.getId() );
        //            }
        //
        //            logger.warn( "\n\n\n\nCYCLE DETECTED!\n\nCycle: %s\n\n\n\n", convertToRelationships( p.relationships() ) );
        //            cycles.add( cycle );
        //        }
        //
        //        logger.info( "\n\n\n\n%d CYCLES via: %s\n\n\n\n", cycles.size(), rel );
        //        return cycles;
    }

    private Node newProjectNode( final ProjectVersionRef ref )
    {
        final Node node = graph.createNode();
        toNodeProperties( ref, node, false );

        final String gav = ref.toString();

        graph.index()
             .forNodes( ALL_NODES )
             .add( node, GAV, gav );

        graph.index()
             .forNodes( MISSING_NODES_IDX )
             .add( node, GAV, gav );

        if ( ref.isVariableVersion() )
        {
            logger.info( "Adding %s to variable-nodes index.", ref );
            graph.index()
                 .forNodes( VARIABLE_NODES_IDX )
                 .add( node, GAV, gav );
        }

        return node;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        Iterable<Node> nodes = null;
        if ( roots != null && !roots.isEmpty() )
        {
            final RootedNodesCollector agg = new RootedNodesCollector( roots, filter, false );
            collectAtlasRelationships( agg, roots );
            nodes = agg;
        }
        else
        {
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( ALL_NODES )
                                              .query( GAV, "*" );
            nodes = hits;
        }

        return new HashSet<ProjectVersionRef>( convertToProjects( nodes ) );
    }

    private Set<Node> getAllProjectNodes()
    {
        if ( roots != null && !roots.isEmpty() )
        {
            final RootedNodesCollector agg = new RootedNodesCollector( roots, filter, false );
            collectAtlasRelationships( agg, roots );
            return agg.getFoundNodes();
        }
        else
        {
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( ALL_NODES )
                                              .query( GAV, "*" );
            return toSet( hits );
        }
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
        if ( filter == null )
        {
            return GraphRelType.atlasRelationshipTypes();
        }

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
        else
        {
            result.addAll( GraphRelType.atlasRelationshipTypes() );
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
        final EndNodesCollector checker = new EndNodesCollector( roots, Collections.singleton( node ), filter, false );

        collectAtlasRelationships( checker, roots );
        return checker.hasFoundNodes();
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
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .query( GAV, "*" );

        return hasIndexedProjects( hits );
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .query( GAV, "*" );

        return getIndexedProjects( hits );
        //        return getAllFlaggedProjects( CONNECTED, false );
    }

    private Set<ProjectVersionRef> getIndexedProjects( final Iterable<Node> hits )
    {
        final Set<Node> nodes = toSet( hits );
        final EndNodesCollector checker = new EndNodesCollector( roots, nodes, filter, false );

        collectAtlasRelationships( checker, roots );

        final Set<Node> found = checker.getFoundNodes();
        //        logger.info( "Found %d nodes: %s", found.size(), found );

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final Node node : found )
        {
            refs.add( toProjectVersionRef( node ) );
        }

        return refs;
    }

    private boolean hasIndexedProjects( final Iterable<Node> hits )
    {
        final Set<Node> nodes = toSet( hits );
        final EndNodesCollector checker = new EndNodesCollector( roots, nodes, filter, true );

        collectAtlasRelationships( checker, roots );

        return checker.hasFoundNodes();
    }

    private void collectAtlasRelationships( final AtlasCollector<?> checker, final Set<Node> from )
    {
        if ( from == null || from.isEmpty() )
        {
            throw new UnsupportedOperationException(
                                                     "Cannot collect atlas nodes/relationships via traversal without at least one 'from' node!" );
        }

        //        logger.info( "Traversing for aggregation using: %s from roots: %s", checker.getClass()
        //                                                                                   .getName(), from );

        TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL );
        //                                                    .sort( new PathComparator() );

        final Set<GraphRelType> relTypes = getRelTypes( filter );
        for ( final GraphRelType grt : relTypes )
        {
            description.relationships( grt, Direction.OUTGOING );
        }

        description = description.breadthFirst();

        description = description.expand( checker )
                                 .evaluator( checker );

        final Traverser traverser = description.traverse( from.toArray( new Node[] {} ) );
        for ( @SuppressWarnings( "unused" )
        final Path path : traverser )
        {
            //            logger.info( "Aggregating path: %s", path );
            // Don't need this, but we need to iterate the traverser.
        }
    }

    public boolean hasVariableProjects()
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( VARIABLE_NODES_IDX )
                                          .query( GAV, "*" );

        return hasIndexedProjects( hits );
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( VARIABLE_NODES_IDX )
                                          .query( GAV, "*" );

        //        logger.info( "Getting variable projects" );
        return getIndexedProjects( hits );
        //        return getAllFlaggedProjects( VARIABLE, true );
    }

    public boolean addCycle( final EProjectCycle cycle )
    {
        // NOP, auto-detected.
        return false;
    }

    public Set<EProjectCycle> getCycles()
    {
        printCaller( "GET-CYCLES" );

        final IndexHits<Relationship> hits = graph.index()
                                                  .forRelationships( CYCLE_INJECTION_IDX )
                                                  .query( RELATIONSHIP_ID, "*" );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        for ( final Relationship hit : hits )
        {
            if ( hasPathTo( hit.getStartNode() ) )
            {
                final Set<Set<Long>> cycleIds = getInjectedCycles( hit );
                nextCycle: for ( final Set<Long> cycle : cycleIds )
                {
                    final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
                    for ( final Long relId : cycle )
                    {
                        final Relationship r = graph.getRelationshipById( relId );
                        if ( r == null )
                        {
                            continue nextCycle;
                        }

                        rels.add( toProjectRelationship( r ) );
                    }

                    cycles.add( new EProjectCycle( rels ) );
                }
            }
        }

        if ( filter != null )
        {
            nextCycle: for ( final Iterator<EProjectCycle> it = cycles.iterator(); it.hasNext(); )
            {
                final EProjectCycle eProjectCycle = it.next();
                ProjectRelationshipFilter f = filter;
                for ( final ProjectRelationship<?> rel : eProjectCycle )
                {
                    if ( !f.accept( rel ) )
                    {
                        it.remove();
                        continue nextCycle;
                    }

                    f = f.getChildFilter( rel );
                }
            }
        }

        return cycles;

        //        final Map<String, Object> params = new HashMap<String, Object>();
        //        params.put( "roots", ( roots == null || roots.isEmpty() ? "*" : roots ) );
        //
        //        final ExecutionResult result = execute( CYPHER_CYCLE_RETRIEVAL, params );
        //
        //        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        //        nextPath: for ( final Map<String, Object> record : result )
        //        {
        //            final Path p = (Path) record.get( "path" );
        //            final Node terminus = p.lastRelationship()
        //                                   .getEndNode();
        //
        //            final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        //            boolean logging = false;
        //
        //            ProjectRelationshipFilter f = filter;
        //            for ( final Relationship r : p.relationships() )
        //            {
        //                if ( r.getStartNode()
        //                      .equals( terminus ) )
        //                {
        //                    logging = true;
        //                }
        //
        //                if ( logging )
        //                {
        //                    final ProjectRelationship<?> rel = toProjectRelationship( r );
        //                    if ( f != null )
        //                    {
        //                        if ( !f.accept( rel ) )
        //                        {
        //                            continue nextPath;
        //                        }
        //                        else
        //                        {
        //                            f = f.getChildFilter( rel );
        //                        }
        //                    }
        //
        //                    rels.add( rel );
        //                }
        //            }
        //
        //            if ( !rels.isEmpty() )
        //            {
        //                cycles.add( new EProjectCycle( rels ) );
        //            }
        //        }
        //
        //        return cycles;
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
        final Node node = getNode( ref, false );
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

        final Set<Node> connected = new HashSet<Node>();
        for ( final Node node : nodes )
        {
            // TODO: What about disconnected nodes discovered as part of the graph??
            if ( hasPathTo( node ) )
            {
                connected.add( node );
            }
        }

        return new HashSet<ProjectVersionRef>( convertToProjects( connected ) );
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

        final EndGAVsPathsCollector checker =
            new EndGAVsPathsCollector( roots, Collections.singleton( variable ), filter, false );

        collectAtlasRelationships( checker, roots );

        for ( final Path p : checker )
        {
            final Relationship from = p.lastRelationship();
            final ProjectRelationship<?> rel = toProjectRelationship( from );
            final ProjectRelationship<?> sel = rel.selectTarget( (SingleVersion) select.getVersionSpec() );

            selectRelationship( p.startNode(), from, sel );
        }
    }

    private Relationship selectRelationship( final Node root, final Relationship from, final ProjectRelationship<?> toPR )
    {
        //        logger.info( "\n\n\n\nSELECT: %s\n\n\n\n", toPR );
        Relationship to = null;
        Transaction tx = null;
        try
        {
            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            final String toId = id( toPR );

            Node fromNode = from.getStartNode();

            tx = graph.beginTx();

            //            logger.info( "\n\nLooking for relationship with id: %s", toId );
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
                    //                    logger.info( "Created node %s for selected project version: %s", toNode, toPR.getTarget()
                    //                                                                                                 .asProjectVersionRef() );
                }

                //                logger.info( "\n\nCreating relationship for selected: %s from node: %s to node: %s\n\n", toPR,
                //                             fromNode, toNode );

                to = fromNode.createRelationshipTo( toNode, from.getType() );

                cloneRelationshipProperties( from, to );
                relIdx.add( to, RELATIONSHIP_ID, toId );

                markConnected( fromNode, true );
                markSelectedFor( to, root );
            }
            else
            {
                to = hits.next();
                //                logger.info( "Got relationship: %s", to );
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

        //        logger.info( "SELECTION DONE; returning: %s", to );

        return to;
    }

    public Map<ProjectVersionRef, ProjectVersionRef> clearSelectedVersions()
        throws GraphDriverException
    {
        final Set<SelectionInfo> infos = getSelectionInfo();

        final Map<ProjectVersionRef, ProjectVersionRef> clearedMap = createVariableToSelectedMap( infos );

        //        logger.info( "Got variable -> selected mappings:\n\n", join( clearedMap.entrySet(), "\n" ) );

        Transaction tx = null;
        final Set<Long> deleted = new HashSet<Long>();
        try
        {
            for ( final SelectionInfo info : infos )
            {
                logger.debug( "Clearing selection:\nSelected: %s\nVariable: %s", info.getSelectedRelationship()
                                                                                     .getEndNode()
                                                                                     .getProperty( Conversions.GAV ),
                              info.getVariableRelationship()
                                  .getEndNode()
                                  .getProperty( Conversions.GAV ) );

                if ( tx == null )
                {
                    tx = graph.beginTx();
                }

                final long srId = info.getSelectedRelationship()
                                      .getId();
                final long vrId = info.getVariableRelationship()
                                      .getId();
                if ( deleted.contains( srId ) || deleted.contains( vrId ) )
                {
                    logger.debug( "Selected- or Variable-relationship already deleted:\n  selected: %s\n    deleted? %s\n  variable: %s\n    deleted? %s.\nContinuing to next mapping.",
                                  info.getSelectedRelationship(), deleted.contains( srId ),
                                  info.getVariableRelationship(), deleted.contains( vrId ) );
                    continue;
                }

                if ( isCloneFor( info.getSelectedRelationship(), info.getVariableRelationship() ) )
                {
                    logger.debug( "Deleting cloned relationship from previous selection operation: %s",
                                  info.getSelectedRelationship() );
                    deleted.add( srId );
                    info.getSelectedRelationship()
                        .delete();
                }

                for ( final Node root : roots )
                {
                    removeSelectionAnnotationsFor( info.getVariableRelationship(), root );
                    if ( !deleted.contains( srId ) )
                    {
                        logger.debug( "Removing selection annotations for previously selected: %s",
                                      info.getSelectedRelationship() );
                        markDeselectedFor( info.getSelectedRelationship(), root );
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
            result.put( toProjectVersionRef( info.getVariable() ), toProjectVersionRef( info.getSelected() ) );
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

        final SelectionFinderAtlasCollector collector = new SelectionFinderAtlasCollector( roots, filter );
        collectAtlasRelationships( collector, roots );

        return collector.getSelectionInfos();

        //        final Map<String, Object> params = new HashMap<String, Object>();
        //        params.put( "roots", roots );
        //
        //        final ExecutionResult result = execute( CYPHER_SELECTION_RETRIEVAL, params );
        //
        //        final Iterator<Map<String, Object>> mapIt = result.iterator();
        //
        //        final Set<SelectionInfo> selected = new HashSet<SelectionInfo>();
        //        while ( mapIt.hasNext() )
        //        {
        //            final Map<String, Object> record = mapIt.next();
        //            final Node v = (Node) record.get( "v" );
        //            final Node s = (Node) record.get( "s" );
        //            final Relationship sr = (Relationship) record.get( "r1" );
        //            final Relationship vr = (Relationship) record.get( "r2" );
        //
        //            if ( s == null || vr == null || sr == null )
        //            {
        //                logger.error( "Found de-selected: %s with missing selected project, variable relationship, or selected relationship!",
        //                              ( v.hasProperty( GAV ) ? v.getProperty( GAV ) + "(" + v.getId() + ")" : v.getId() ) );
        //                continue;
        //            }
        //
        //            selected.add( new SelectionInfo( v, vr, s, sr ) );
        //        }
        //
        //        return selected;
    }

    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !containsProject( ref ) )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                newProjectNode( ref );

                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
    }

}
