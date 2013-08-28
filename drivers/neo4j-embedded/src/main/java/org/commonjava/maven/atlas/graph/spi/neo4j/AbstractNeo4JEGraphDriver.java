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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.FORCE_VERSION_SELECTIONS;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GA;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RELATIONSHIP_ID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.SOURCE_URI;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.WS_ID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.addToURIListProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.clearCloneStatus;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.cloneRelationshipProperties;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.convertToProjects;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.convertToRelationships;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.convertToWorkspaces;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getBooleanProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getInjectedCycles;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getMetadataMap;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getSelections;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getStringProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getWorkspaceSelections;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.id;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isCloneFor;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isConnected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.markConnected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.markCycleInjection;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.markDeselected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.markSelection;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.markSelectionOnly;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.removeSelectionAnnotationsFor;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toNodeProperties;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectVersionRef;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toRelationshipProperties;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toSelectionNodeProperties;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toSet;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toWorkspace;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.getGraphRelTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.ConnectingPathsCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.CycleDetectingCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.EndNodesCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.MembershipWrappedTraversalEvaluator;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.NodePair;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.RootedNodesCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.RootedRelationshipsCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils;
import org.commonjava.maven.atlas.graph.traverse.AbstractFilteringTraversal;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
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
    implements Runnable, Neo4JEGraphDriver
{

    private final Logger logger = new Logger( getClass() );

    private static final String ALL_RELATIONSHIPS = "all_relationships";

    private static final String BY_GAV_IDX = "by_gav";

    private static final String BY_GA_IDX = "by_ga";

    private static final String GA_SELECTIONS_IDX = "ga_selections";

    private static final String ALL_WORKSPACES = "all_workspaces";

    private static final String CYCLE_INJECTION_IDX = "cycle_injections";

    private static final String VARIABLE_NODES_IDX = "variable_nodes";

    private static final String MISSING_NODES_IDX = "missing_nodes";

    private static final String METADATA_INDEX_PREFIX = "has_metadata_";

    private static final String CACHED_ALL_PROJECT_REFS = "all-project-refs";

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

    private final boolean useShutdownHook;

    private ExecutionEngine queryEngine;

    private final Map<GraphView, Map<String, Object>> caches = new WeakHashMap<>();

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

    protected boolean isUseShutdownHook()
    {
        return useShutdownHook;
    }

    private void printGraphStats()
    {
        final Logger logger = new Logger( getClass() );
        logger.info( "Loaded approximately %d nodes.", graph.index()
                                                            .forNodes( BY_GAV_IDX )
                                                            .query( GAV, "*" )
                                                            .size() );

        logger.info( "Loaded approximately %d relationships.", graph.index()
                                                                    .forRelationships( ALL_RELATIONSHIPS )
                                                                    .query( RELATIONSHIP_ID, "*" )
                                                                    .size() );
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final GraphView view, final ProjectVersionRef ref )
    {
        checkClosed();

        if ( ref == null )
        {
            return null;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
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

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final GraphView view, final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
        final IndexHits<Node> hits = index.get( GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        final Set<Node> roots = getRoots( view );
        if ( roots != null && !roots.isEmpty() )
        {
            final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();

            final RootedRelationshipsCollector checker = new RootedRelationshipsCollector( roots, view, false );

            collectAtlasRelationships( view, checker, roots, false );

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

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final GraphView view, final ProjectVersionRef... refs )
    {
        // NOTE: using global lookup here to avoid checking for paths, which we're going to collect below.
        final Set<Node> nodes = new HashSet<Node>( refs.length );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node n = getNode( ref );
            if ( n != null )
            {
                nodes.add( n );
            }
        }

        if ( nodes.isEmpty() )
        {
            return null;
        }

        final Set<Node> roots = getRoots( view );
        final ConnectingPathsCollector checker = new ConnectingPathsCollector( roots, nodes, view, false );

        collectAtlasRelationships( view, checker, roots, false );

        final Set<Path> paths = checker.getFoundPaths();
        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        for ( final Path path : paths )
        {
            result.add( convertToRelationships( path.relationships() ) );
        }

        return result;
    }

    private Set<Path> getPathsTo( final GraphView view, final Set<Node> nodes )
    {
        final Set<Node> roots = getRoots( view );
        final ConnectingPathsCollector checker = new ConnectingPathsCollector( roots, nodes, view, false );

        collectAtlasRelationships( view, checker, roots, false );

        return checker.getFoundPaths();
    }

    @Override
    public synchronized Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rels )
    {
        checkClosed();

        Transaction tx = graph.beginTx();
        final Map<ProjectRelationship<?>, Relationship> potentialCycleInjectors = new HashMap<>();
        try
        {
            for ( final ProjectRelationship<?> rel : rels )
            {
                logger.debug( "Adding relationship: %s", rel );

                final Index<Node> index = graph.index()
                                               .forNodes( BY_GAV_IDX );

                final ProjectVersionRef declaring = rel.getDeclaring();
                final ProjectVersionRef target = rel.getTarget()
                                                    .asProjectVersionRef();

                final Node[] nodes = new Node[2];
                int i = 0;
                for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
                {
                    final IndexHits<Node> hits = index.get( GAV, ref.toString() );
                    if ( !hits.hasNext() )
                    {
                        logger.debug( "Creating new node for: %s to support addition of relationship: %s", ref, rel );
                        final Node node = newProjectNode( ref );
                        nodes[i] = node;
                    }
                    else
                    {
                        nodes[i] = hits.next();

                        logger.debug( "Using existing project node: %s", nodes[i] );
                    }

                    i++;
                }

                final RelationshipIndex relIdx = graph.index()
                                                      .forRelationships( ALL_RELATIONSHIPS );

                final String relId = id( rel );
                final IndexHits<Relationship> relHits = relIdx.get( RELATIONSHIP_ID, relId );

                Relationship relationship;
                if ( relHits.size() < 1 )
                {
                    final Node from = nodes[0];

                    if ( from != nodes[1] )
                    {
                        final Node to = nodes[1];

                        logger.debug( "Creating graph relationship for: %s between node: %d and node: %d", rel, from, to );

                        final GraphRelType grt = GraphRelType.map( rel.getType(), rel.isManaged() );

                        relationship = from.createRelationshipTo( to, grt );

                        logger.debug( "New relationship is: %s with type: %s", relationship, grt );

                        toRelationshipProperties( rel, relationship );
                        relIdx.add( relationship, RELATIONSHIP_ID, relId );

                        final IndexHits<Node> gaSelectionHits = graph.index()
                                                                     .forNodes( GA_SELECTIONS_IDX )
                                                                     .query( GA, target.asProjectRef()
                                                                                       .toString() );
                        if ( gaSelectionHits.hasNext() )
                        {
                            final Map<String, SingleVersion> workspaceSelections = getWorkspaceSelections( gaSelectionHits.next() );

                            if ( workspaceSelections != null )
                            {
                                for ( final Entry<String, SingleVersion> entry : workspaceSelections.entrySet() )
                                {
                                    final String key = entry.getKey();
                                    final SingleVersion value = entry.getValue();

                                    selectRelationship( getWorkspaceNodeId( key ), relationship, value );
                                }
                            }
                        }

                        logger.debug( "Created relationship: %s (%s)", relationship, toProjectRelationship( relationship ) );
                    }
                    else
                    {
                        continue;
                    }

                    logger.debug( "Removing missing/incomplete flag from: %s (%s)", from, declaring );
                    graph.index()
                         .forNodes( MISSING_NODES_IDX )
                         .remove( from );

                    markConnected( from, true );
                }
                else
                {
                    relationship = relHits.next();
                    logger.debug( "Reusing existing relationship: %s (%s)", relationship, toProjectRelationship( relationship ) );

                    clearCloneStatus( relationship );
                    addToURIListProperty( rel.getSources(), SOURCE_URI, relationship );
                    markSelectionOnly( relationship, false );
                }

                potentialCycleInjectors.put( rel, relationship );
            }

            logger.debug( "Committing graph transaction." );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        tx = graph.beginTx();
        final Set<ProjectRelationship<?>> skipped = new HashSet<ProjectRelationship<?>>();
        try
        {
            logger.debug( "Analyzing for new cycles..." );
            skipped.addAll( markCycles( potentialCycleInjectors ) );

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        logger.debug( "Cycle injection detected for: %s", skipped );

        updateCaches( skipped, rels );

        return skipped;
    }

    private void updateCaches( final Set<ProjectRelationship<?>> skipped, final ProjectRelationship<?>[] rels )
    {
        if ( rels.length == skipped.size() )
        {
            return;
        }

        final Set<ProjectRelationship<?>> adds = new HashSet<>( Arrays.asList( rels ) );
        adds.removeAll( skipped );

        for ( final Map<String, Object> cacheMap : new HashSet<>( caches.values() ) )
        {
            @SuppressWarnings( "unchecked" )
            final Set<ProjectVersionRef> cachedRefs = (Set<ProjectVersionRef>) cacheMap.get( CACHED_ALL_PROJECT_REFS );
            if ( cachedRefs != null )
            {
                synchronized ( cachedRefs )
                {
                    for ( final ProjectRelationship<?> add : adds )
                    {
                        cachedRefs.add( add.getDeclaring()
                                           .asProjectVersionRef() );

                        cachedRefs.add( add.getTarget()
                                           .asProjectVersionRef() );
                    }
                }
            }
        }
    }

    private Set<ProjectRelationship<?>> markCycles( final Map<ProjectRelationship<?>, Relationship> potentialCycleInjectors )
    {
        final Map<ProjectRelationship<?>, Set<List<Relationship>>> cycleMap = getIntroducedCycles( GraphView.GLOBAL, potentialCycleInjectors );

        final Set<ProjectRelationship<?>> cycleInjectors = new HashSet<>();
        if ( cycleMap != null && !cycleMap.isEmpty() )
        {
            for ( final Entry<ProjectRelationship<?>, Set<List<Relationship>>> entry : cycleMap.entrySet() )
            {
                final ProjectRelationship<?> rel = entry.getKey();
                final Relationship relationship = potentialCycleInjectors.get( rel );

                final Set<List<Relationship>> cycles = entry.getValue();

                if ( cycles != null && !cycles.isEmpty() )
                {
                    markCycleInjection( relationship, cycles );

                    final String relId = id( rel );
                    graph.index()
                         .forRelationships( CYCLE_INJECTION_IDX )
                         .add( relationship, RELATIONSHIP_ID, relId );

                    cycleInjectors.add( rel );
                }
            }
        }

        return cycleInjectors;
    }

    @Override
    public boolean introducesCycle( final GraphView view, final ProjectRelationship<?> rel )
    {
        return !getIntroducedCycles( view, Collections.<ProjectRelationship<?>, Relationship> singletonMap( rel, null ) ).isEmpty();
    }

    private Map<ProjectRelationship<?>, Set<List<Relationship>>> getIntroducedCycles( final GraphView view,
                                                                                      final Map<ProjectRelationship<?>, Relationship> potentialCycleInjectors )
    {
        final Map<NodePair, ProjectRelationship<?>> src = new HashMap<>();
        final Set<Node> targets = new HashSet<>();
        for ( final Map.Entry<ProjectRelationship<?>, Relationship> entry : potentialCycleInjectors.entrySet() )
        {
            final ProjectRelationship<?> rel = entry.getKey();
            final Relationship r = entry.getValue();

            final Node from;
            final Node to;
            if ( r != null )
            {
                from = r.getStartNode();
                to = r.getEndNode();
            }
            else
            {
                from = getNode( rel.getDeclaring()
                                   .asProjectVersionRef() );
                to = getNode( rel.getTarget()
                                 .asProjectVersionRef() );
            }

            targets.add( to );

            //            logger.info( "Listening for cycle introduced by: %s. (looking for path from: %s to: %s)", rel, to.getId(), from.getId() );
            src.put( new NodePair( to, from ), rel );
        }

        if ( src.isEmpty() )
        {
            return Collections.emptyMap();
        }

        final CycleDetectingCollector checker = new CycleDetectingCollector( src );
        collectAtlasRelationships( view, checker, targets, false );

        return checker.getFoundPathMap();
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
             .forNodes( BY_GAV_IDX )
             .add( node, GAV, gav );

        graph.index()
             .forNodes( BY_GA_IDX )
             .add( node, GA, ref.asProjectRef()
                                .toString() );

        graph.index()
             .forNodes( MISSING_NODES_IDX )
             .add( node, GAV, gav );

        if ( ref.isVariableVersion() )
        {
            logger.debug( "Adding %s to variable-nodes index.", ref );
            graph.index()
                 .forNodes( VARIABLE_NODES_IDX )
                 .add( node, GAV, gav );
        }

        logger.debug( "Created project node: %s with id: %d", ref, node.getId() );
        return node;
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        final Set<Node> roots = getRoots( view );
        Iterable<Node> nodes = null;
        if ( roots != null && !roots.isEmpty() )
        {
            final RootedNodesCollector agg = new RootedNodesCollector( roots, view, false );
            collectAtlasRelationships( view, agg, roots, false );
            nodes = agg;
        }
        else
        {
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( BY_GAV_IDX )
                                              .query( GAV, "*" );
            nodes = hits;
        }

        return new HashSet<ProjectVersionRef>( convertToProjects( nodes ) );
    }

    private Set<Node> getAllProjectNodes( final GraphView view )
    {
        final Set<Node> roots = getRoots( view );
        if ( roots != null && !roots.isEmpty() )
        {
            final RootedNodesCollector agg = new RootedNodesCollector( roots, view, false );
            collectAtlasRelationships( view, agg, roots, false );
            return agg.getFoundNodes();
        }
        else
        {
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( BY_GAV_IDX )
                                              .query( GAV, "*" );
            return toSet( hits );
        }
    }

    @Override
    public void traverse( final GraphView view, final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
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

            final Set<Long> rootIds = getRootIds( view );

            @SuppressWarnings( { "rawtypes", "unchecked" } )
            final MembershipWrappedTraversalEvaluator checker = new MembershipWrappedTraversalEvaluator( rootIds, traversal, i );

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
            relTypes.addAll( getGraphRelTypes( rootFilter ) );
        }
        else
        {
            relTypes.addAll( Arrays.asList( GraphRelType.values() ) );
        }

        return relTypes;
    }

    private void printCaller( final String label )
    {
        //        logger.debug( "\n\n\n\n%s called from:\n\n%s\n\n\n\n", label, join( new Throwable().getStackTrace(), "\n" ) );
    }

    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        if ( view != null )
        {
            Map<String, Object> cacheMap;
            synchronized ( caches )
            {
                cacheMap = caches.get( view );
                if ( cacheMap == null )
                {
                    cacheMap = new HashMap<>();
                    caches.put( view, cacheMap );
                }
            }

            @SuppressWarnings( "unchecked" )
            Set<ProjectVersionRef> cachedRefs = (Set<ProjectVersionRef>) cacheMap.get( CACHED_ALL_PROJECT_REFS );
            if ( cachedRefs == null )
            {
                cachedRefs = getAllProjects( view );
                cacheMap.put( CACHED_ALL_PROJECT_REFS, cachedRefs );
            }

            return cachedRefs.contains( ref );
        }

        return getNode( view, ref ) != null;
    }

    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        return getRelationship( rel ) != null;
    }

    @Override
    public Node getNode( final ProjectVersionRef ref )
    {
        return getNode( null, ref );
    }

    public Node getNode( final GraphView view, final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( BY_GAV_IDX );

        final IndexHits<Node> hits = idx.get( GAV, ref.asProjectVersionRef()
                                                      .toString() );

        final Node node = hits.hasNext() ? hits.next() : null;

        //        logger.debug( "Query result for node: %s is: %s\nChecking for path to root(s): %s", ref, node,
        //                      join( roots, "|" ) );

        if ( view != null && !hasPathTo( view, node ) )
        {
            return null;
        }

        return node;
    }

    private boolean hasPathTo( final GraphView view, final Node node )
    {
        if ( node == null )
        {
            return false;
        }

        final GraphView v = view == null ? GraphView.GLOBAL : view;

        final Set<Node> roots = getRoots( v );
        if ( roots == null || roots.isEmpty() )
        {
            return true;
        }

        if ( roots.contains( node.getId() ) )
        {
            return true;
        }

        logger.debug( "Checking for path between roots: %s and target node: %s", join( roots, "," ), node.getId() );
        final EndNodesCollector checker = new EndNodesCollector( roots, Collections.singleton( node ), view, false );

        collectAtlasRelationships( v, checker, roots, false );
        return checker.hasFoundNodes();
    }

    @Override
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

    @Override
    public synchronized void close()
        throws IOException
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

    @Override
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

    @SuppressWarnings( "unused" )
    private boolean isMissing( final Node node )
    {
        return !isConnected( node );
    }

    @Override
    public boolean isMissing( final GraphView view, final ProjectVersionRef ref )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( BY_GAV_IDX )
                                          .get( GAV, ref.toString() );

        if ( hits.size() > 0 )
        {
            return !isConnected( hits.next() );
        }

        return false;
    }

    @Override
    public boolean hasMissingProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .query( GAV, "*" );

        return hasIndexedProjects( view, hits );
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .query( GAV, "*" );

        return getIndexedProjects( view, hits );
        //        return getAllFlaggedProjects( CONNECTED, false );
    }

    private Set<ProjectVersionRef> getIndexedProjects( final GraphView view, final Iterable<Node> hits )
    {
        final Set<Node> nodes = toSet( hits );
        final Set<Node> roots = getRoots( view );
        final EndNodesCollector checker = new EndNodesCollector( roots, nodes, view, false );

        collectAtlasRelationships( view, checker, roots, false );

        final Set<Node> found = checker.getFoundNodes();
        //        logger.info( "Found %d nodes: %s", found.size(), found );

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final Node node : found )
        {
            refs.add( toProjectVersionRef( node ) );
        }

        return refs;
    }

    private boolean hasIndexedProjects( final GraphView view, final Iterable<Node> hits )
    {
        final Set<Node> nodes = toSet( hits );
        final Set<Node> roots = getRoots( view );
        final EndNodesCollector checker = new EndNodesCollector( roots, nodes, view, true );

        collectAtlasRelationships( view, checker, roots, false );

        return checker.hasFoundNodes();
    }

    private Set<Node> getRoots( final GraphView view )
    {
        final Set<ProjectVersionRef> rootRefs = view.getRoots();
        if ( rootRefs == null || rootRefs.isEmpty() )
        {
            return null;
        }

        final Set<Node> nodes = new HashSet<Node>( rootRefs.size() );
        for ( final ProjectVersionRef ref : rootRefs )
        {
            final Node n = getNode( ref );
            if ( n != null )
            {
                nodes.add( n );
            }
        }

        return nodes;
    }

    private Set<Long> getRootIds( final GraphView view )
    {
        final Set<Node> rootNodes = getRoots( view );
        if ( rootNodes == null )
        {
            return null;
        }

        final Set<Long> ids = new HashSet<Long>( rootNodes.size() );
        for ( final Node node : rootNodes )
        {
            ids.add( node.getId() );
        }

        return ids;
    }

    private void collectAtlasRelationships( final GraphView view, final AtlasCollector<?> checker, final Set<Node> from, final boolean sorted )
    {
        if ( from == null || from.isEmpty() )
        {
            throw new UnsupportedOperationException( "Cannot collect atlas nodes/relationships via traversal without at least one 'from' node!" );
        }

        //        logger.info( "Traversing for aggregation using: %s from roots: %s", checker.getClass()
        //                                                                                   .getName(), from );

        TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL );
        if ( sorted )
        {
            description = description.sort( new PathComparator() );
        }

        final Set<GraphRelType> relTypes = getGraphRelTypes( view.getFilter() );
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

    @Override
    public boolean hasVariableProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( VARIABLE_NODES_IDX )
                                          .query( GAV, "*" );

        return hasIndexedProjects( view, hits );
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( VARIABLE_NODES_IDX )
                                          .query( GAV, "*" );

        //        logger.info( "Getting variable projects" );
        return getIndexedProjects( view, hits );
        //        return getAllFlaggedProjects( VARIABLE, true );
    }

    @Override
    public boolean addCycle( final EProjectCycle cycle )
    {
        // NOP, auto-detected.
        return false;
    }

    @Override
    public Set<EProjectCycle> getCycles( final GraphView view )
    {
        printCaller( "GET-CYCLES" );

        final IndexHits<Relationship> hits = graph.index()
                                                  .forRelationships( CYCLE_INJECTION_IDX )
                                                  .query( RELATIONSHIP_ID, "*" );

        final Map<Node, Relationship> targetNodes = new HashMap<>();
        for ( final Relationship hit : hits )
        {
            targetNodes.put( hit.getStartNode(), hit );
        }

        final Set<Path> paths = getPathsTo( view, targetNodes.keySet() );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        for ( final Path path : paths )
        {
            final Node node = path.endNode();
            if ( node == null )
            {
                logger.error( "Path to cycle has no end-node: %s", path );
                continue;
            }

            final Relationship hit = targetNodes.get( node );

            final Set<List<Long>> cycleIds = getInjectedCycles( hit );
            nextCycle: for ( final List<Long> cycle : cycleIds )
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

                ProjectRelationshipFilter f = view.getFilter();
                if ( f != null )
                {
                    for ( final ProjectRelationship<?> rel : rels )
                    {
                        if ( !f.accept( rel ) )
                        {
                            continue nextCycle;
                        }

                        f = f.getChildFilter( rel );
                    }
                }

                cycles.add( new EProjectCycle( rels ) );
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

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : getCycles( view ) )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : getCycles( view ) )
        {
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void recomputeIncompleteSubgraphs()
    {
        // NOP, handled automatically.
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return null;
        }

        return getMetadataMap( node );
    }

    @Override
    public synchronized void addMetadata( final ProjectVersionRef ref, final String key, final String value )
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

    @Override
    public synchronized void setMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
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

    @Override
    public ExecutionResult executeFrom( final String cypher, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        return executeFrom( cypher, null, roots );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        if ( cypher.startsWith( "START" ) )
        {
            throw new GraphDriverException( "Leave off the START clause when supplying ProjectVersionRef instances as query roots:\n'%s'", cypher );
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

    @Override
    public ExecutionResult executeFrom( final String cypher, final ProjectRelationship<?> rootRel )
        throws GraphDriverException
    {
        return executeFrom( cypher, null, rootRel );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params, final ProjectRelationship<?> rootRel )
        throws GraphDriverException
    {
        if ( cypher.startsWith( "START" ) )
        {
            throw new GraphDriverException( "Leave off the START clause when supplying ProjectRelationship instances as query roots:\n'%s'", cypher );
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

    @Override
    public ExecutionResult execute( final String cypher )
    {
        return execute( cypher, null );
    }

    @Override
    public ExecutionResult execute( final String cypher, final Map<String, Object> params )
    {
        checkExecutionEngine();

        logger.debug( "Running query:\n\n%s\n\nWith params:\n\n%s\n\n", cypher, params );

        final String query = cypher.replaceAll( "(\\s)\\s+", "$1" );

        final ExecutionResult result = params == null ? queryEngine.execute( query ) : queryEngine.execute( query, params );

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

    @Override
    public synchronized void reindex()
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Iterable<Node> nodes = getAllProjectNodes( GraphView.GLOBAL );
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

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
    {
        final IndexHits<Node> nodes = graph.index()
                                           .forNodes( METADATA_INDEX_PREFIX + key )
                                           .query( GAV, "*" );

        final Set<Node> targetNodes = new HashSet<Node>();
        for ( final Node node : nodes )
        {
            targetNodes.add( node );
        }

        final Set<Path> paths = getPathsTo( view, targetNodes );
        final Set<Node> connected = new HashSet<Node>();
        nextPath: for ( final Path path : paths )
        {
            ProjectRelationshipFilter f = view.getFilter();
            if ( f != null )
            {
                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships() );
                for ( final ProjectRelationship<?> rel : rels )
                {
                    if ( !f.accept( rel ) )
                    {
                        continue nextPath;
                    }

                    f = f.getChildFilter( rel );
                }
            }

            connected.add( path.endNode() );
        }

        return new HashSet<ProjectVersionRef>( convertToProjects( connected ) );
    }

    @Override
    public void selectVersionFor( final ProjectVersionRef variable, final SingleVersion select, final String sessionId )
    {
        logger.debug( "\n\n\n\nSELECT: %s for: %s\n\n\n\n", select, variable );
        if ( !select.isConcrete() )
        {
            logger.warn( "Cannot select non-concrete version! Attempted to select: %s", select );
            return;
        }

        //        if ( variable.isRelease() )
        //        {
        //            logger.warn( "Cannot select version if target is already a concrete version! Attempted to select for: %s",
        //                         variable );
        //
        //            return;
        //        }

        final Node node = getNode( variable );
        final Iterable<Relationship> rels = node.getRelationships( Direction.INCOMING );

        final long sid = getWorkspaceNodeId( sessionId );
        for ( final Relationship r : rels )
        {
            selectRelationship( sid, r, select );
        }
    }

    @Override
    public void selectVersionForAll( final ProjectRef variable, final SingleVersion select, final String sessionId )
    {
        logger.debug( "\n\n\n\nSELECT: %s for: %s\n\n\n\n", select, variable );
        if ( !select.isConcrete() )
        {
            logger.warn( "Cannot select non-concrete version! Attempted to select: %s", select );
            return;
        }

        //        if ( variable.isRelease() )
        //        {
        //            logger.warn( "Cannot select version if target is already a concrete version! Attempted to select for: %s",
        //                         variable );
        //
        //            return;
        //        }

        final Iterable<Node> nodes = getNodes( variable );
        for ( final Node node : nodes )
        {
            final Iterable<Relationship> rels = node.getRelationships( Direction.INCOMING );

            final long sid = getWorkspaceNodeId( sessionId );
            for ( final Relationship r : rels )
            {
                selectRelationship( sid, r, select );
            }
        }

        synchronized ( this )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                final IndexHits<Node> hits = graph.index()
                                                  .forNodes( GA_SELECTIONS_IDX )
                                                  .query( GA, variable );
                Node gaSelectionsNode;
                if ( hits.hasNext() )
                {
                    hits.next();
                }
                else
                {
                    gaSelectionsNode = graph.createNode();
                    toSelectionNodeProperties( sessionId, variable, select, gaSelectionsNode );
                }
            }
            finally
            {
                tx.finish();
            }
        }
    }

    private Iterable<Node> getNodes( final ProjectRef variable )
    {
        return graph.index()
                    .forNodes( BY_GA_IDX )
                    .query( GA, variable.toString() );
    }

    private synchronized Relationship selectRelationship( final long wsid, final Relationship from, final SingleVersion select )
    {
        Relationship to = null;
        Transaction tx = null;
        try
        {
            final Node wsNode = graph.getNodeById( wsid );
            final boolean force = getBooleanProperty( FORCE_VERSION_SELECTIONS, wsNode, true );

            //        logger.info( "\n\n\n\nSELECT: %s\n\n\n\n", toPR );
            final ProjectRelationship<?> rel = toProjectRelationship( from );

            final ProjectRelationship<?> sel = rel.selectTarget( select, force );

            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            final String toId = id( sel );

            Node fromNode = from.getStartNode();

            tx = graph.beginTx();

            //            logger.info( "\n\nLooking for relationship with id: %s", toId );
            final IndexHits<Relationship> hits = relIdx.get( RELATIONSHIP_ID, toId );
            if ( hits.size() < 1 )
            {
                Node toNode = getNode( sel.getTarget()
                                          .asProjectVersionRef() );

                if ( toNode == null )
                {
                    logger.debug( "Creating new node to deal with selection of version: %s for: %s", select, rel );
                    toNode = newProjectNode( sel.getTarget()
                                                .asProjectVersionRef() );

                    //                    logger.info( "Created node %s for selected project version: %s", toNode, toPR.getTarget()
                    //                                                                                                 .asProjectVersionRef() );
                }

                //                logger.info( "\n\nCreating relationship for selected: %s from node: %s to node: %s\n\n", toPR,
                //                             fromNode, toNode );

                to = fromNode.createRelationshipTo( toNode, from.getType() );
                markSelectionOnly( to, true );

                cloneRelationshipProperties( from, to );
                relIdx.add( to, RELATIONSHIP_ID, toId );

                markConnected( fromNode, true );
            }
            else
            {
                to = hits.next();
                //                logger.info( "Got relationship: %s", to );
                fromNode = to.getStartNode();
            }

            markSelection( from, to, wsNode );

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

    @Override
    public synchronized boolean clearSelectedVersionsFor( final String sessionId )
    {
        Transaction tx = null;
        try
        {
            tx = graph.beginTx();

            final boolean result = clearSelectedVersions( getWorkspaceNodeId( sessionId ), tx );

            tx.success();

            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    private boolean clearSelectedVersions( final long sessionId, final Transaction tx )
    {
        final Node node = graph.getNodeById( sessionId );
        if ( node == null )
        {
            return false;
        }

        final Map<Long, Long> selections = getSelections( node );

        final Set<Long> deleted = new HashSet<Long>();
        for ( final Entry<Long, Long> entry : selections.entrySet() )
        {
            final Long fromId = entry.getKey();
            final Relationship from = graph.getRelationshipById( fromId );

            final Long toId = entry.getValue();
            final Relationship to = graph.getRelationshipById( toId );

            logger.debug( "Clearing selection:\nSelected: %s\nVariable: %s", to.getEndNode()
                                                                               .getProperty( Conversions.GAV ), from.getEndNode()
                                                                                                                    .getProperty( Conversions.GAV ) );

            if ( deleted.contains( toId ) || deleted.contains( fromId ) )
            {
                logger.debug( "Selected- or Variable-relationship already deleted:\n  selected: %s\n    deleted? %s\n  variable: %s\n    deleted? %s.\nContinuing to next mapping.",
                              to, deleted.contains( toId ), from, deleted.contains( fromId ) );
                continue;
            }

            if ( isCloneFor( to, from ) )
            {
                logger.debug( "Deleting cloned relationship from previous selection operation: %s", to );

                deleted.add( toId );
                to.delete();
            }

            removeSelectionAnnotationsFor( from, node );
            if ( !deleted.contains( toId ) )
            {
                logger.debug( "Removing selection annotations for previously selected: %s", to );

                markDeselected( to, node );
                //                        removeSelectionAnnotationsFor( info.sr, root );
            }
        }

        return true;
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !containsProject( GraphView.GLOBAL, ref ) )
        {
            synchronized ( this )
            {
                final Transaction tx = graph.beginTx();
                try
                {
                    logger.debug( "Creating new node to account for disconnected project: %s", ref );
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

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo, final RelationshipType... types )
    {
        final Node node = getNode( from );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            grts.add( GraphRelType.map( relType, false ) );
            if ( includeManagedInfo )
            {
                grts.add( GraphRelType.map( relType, true ) );
            }
        }

        final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING, grts.toArray( new GraphRelType[grts.size()] ) );

        if ( relationships != null )
        {
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            for ( final Relationship r : relationships )
            {
                if ( TraversalUtils.acceptedInView( r, view ) )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r );
                    if ( rel != null )
                    {
                        result.add( rel );
                    }
                }
            }

            return result;
        }

        return null;
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        logger.debug( "Finding relationships targeting: %s (filter: %s, managed: %s, types: %s)", to, view.getFilter(), includeManagedInfo,
                      Arrays.asList( types ) );
        final Node node = getNode( to );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            grts.add( GraphRelType.map( relType, false ) );
            if ( includeManagedInfo )
            {
                grts.add( GraphRelType.map( relType, true ) );
            }
        }

        logger.debug( "Using graph-relationship types: %s", grts );

        final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING, grts.toArray( new GraphRelType[grts.size()] ) );

        if ( relationships != null )
        {
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            for ( final Relationship r : relationships )
            {
                logger.debug( "Examining relationship: %s", r );
                if ( TraversalUtils.acceptedInView( r, view ) )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r );
                    if ( rel != null )
                    {
                        result.add( rel );
                    }
                }
            }

            return result;
        }

        return null;
    }

    @Override
    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphView eProjectNetView )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( BY_GA_IDX )
                                          .query( GA, projectRef.toString() );
        return new HashSet<ProjectVersionRef>( convertToProjects( hits ) );
    }

    @Override
    public synchronized GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node wsNode = graph.createNode();
            final GraphWorkspace ws = new GraphWorkspace( Long.toString( wsNode.getId() ), config );

            Conversions.toNodeProperties( ws, wsNode );

            graph.index()
                 .forNodes( ALL_WORKSPACES )
                 .add( wsNode, WS_ID, ws.getId() );

            tx.success();

            return ws;
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public synchronized boolean deleteWorkspace( final String id )
    {
        Transaction tx = null;
        try
        {
            tx = graph.beginTx();

            final long nid = getWorkspaceNodeId( id );
            final Node wsNode = graph.getNodeById( nid );
            if ( wsNode == null )
            {
                return false;
            }

            if ( clearSelectedVersions( nid, tx ) )
            {
                graph.index()
                     .forNodes( ALL_WORKSPACES )
                     .remove( wsNode );

                wsNode.delete();
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return true;
    }

    private long getWorkspaceNodeId( final String id )
    {
        return Long.parseLong( id );
    }

    @Override
    public synchronized void storeWorkspace( final GraphWorkspace workspace )
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node wsNode = graph.getNodeById( getWorkspaceNodeId( workspace.getId() ) );
            toNodeProperties( workspace, wsNode );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public GraphWorkspace loadWorkspace( final String id )
        throws GraphDriverException
    {
        final long nid = getWorkspaceNodeId( id );
        final Node node = graph.getNodeById( nid );
        if ( node == null )
        {
            return null;
        }

        return toWorkspace( node );
    }

    @Override
    public Set<GraphWorkspace> loadAllWorkspaces()
    {
        return convertToWorkspaces( graph.index()
                                         .forNodes( ALL_WORKSPACES )
                                         .query( WS_ID, "*" ) );
    }

}
