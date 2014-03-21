/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GA;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RELATIONSHIP_ID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.SOURCE_URI;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.addToURISetProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.convertToProjects;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.convertToRelationships;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getMetadataMap;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getStringProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.id;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isConnected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.markConnected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toNodeProperties;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectVersionRef;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toRelationshipProperties;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toSet;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.getGraphRelTypes;

import java.io.IOException;
import java.net.URI;
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

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.ConnectingPathsCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.CycleDetectingCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.EndNodesCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.MembershipWrappedTraversalEvaluator;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.NodePair;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.RootedNodesCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.RootedPathsCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.RootedRelationshipsCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils;
import org.commonjava.maven.atlas.graph.traverse.AbstractFilteringTraversal;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNeo4JEGraphDriver
    implements Runnable, Neo4JEGraphDriver
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String ALL_RELATIONSHIPS = "all_relationships";

    private static final String ALL_CYCLES = "all_cycles";

    private static final String BY_GAV_IDX = "by_gav";

    private static final String BY_GA_IDX = "by_ga";

    private static final String CONFIG_NODES_IDX = "config_nodes";

    private static final String VARIABLE_NODES_IDX = "variable_nodes";

    private static final String MISSING_NODES_IDX = "missing_nodes";

    private static final String METADATA_INDEX_PREFIX = "has_metadata_";

    private static final String SELECTION_RELATIONSHIPS = "selection_relationships";

    private static final String MANAGED_GA = "managed-ga";

    private static final String MANAGED_KEY = "mkey";

    private static final String PATH_CACHE_PREFIX = "path_cache_for_";

    private static final String REL_CACHE_PREFIX = "rel_cache_for_";

    private static final String NODE_CACHE_PREFIX = "node_cache_for_";

    private static final String CYCLE_CACHE_PREFIX = "cycle_cache_for_";

    private static final String CACHED_PATH_RELATIONSHIP = "cached_path_relationship";

    private static final String CACHED_PATH_CONTAINS_NODE = "cached_path_contains_node";

    private static final String CACHED_PATH_CONTAINS_REL = "cached_path_contains_rel";

    private static final String CACHED_PATH_TARGETS = "cached_path_targets";

    private static final String RID = "rel_id";

    private static final String NID = "node_id";

    private static final String CONFIG_ID = "config_id";

    private static final String BASE_CONFIG_NODE = "_base";

    private static final String VIEW_ID = "view_id";

    private final GraphView globalView;

    //    private static final String GRAPH_ATLAS_TYPES_CLAUSE = join( GraphRelType.atlasRelationshipTypes(), "|" );

    /* @formatter:off */
//    private static final String CYPHER_SELECTION_RETRIEVAL = String.format(
//        "CYPHER 1.8 START a=node({roots}) " 
//            + "\nMATCH p1=(a)-[:{}*1..]->(s), " 
//            + "\n    p2=(a)-[:{}*1..]->(v) "
//            + "\nWITH v, s, last(relationships(p1)) as r1, last(relationships(p2)) as r2 "
//            + "\nWHERE v.{} = s.{} "
//            + "\n    AND v.{} = s.{} "
//            + "\n    AND has(r1.{}) "
//            + "\n    AND any(x in r1.{} "
//            + "\n        WHERE x IN {roots}) "
//            + "\n    AND has(r2.{}) "
//            + "\n    AND any(x in r2.{} "
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

    private final long configNodeId;

    private Node configNode;

    protected AbstractNeo4JEGraphDriver( GraphWorkspaceConfiguration config, final GraphDatabaseService graph, final boolean useShutdownHook )
    {
        this.graph = graph;
        this.useShutdownHook = useShutdownHook;

        printStats();

        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
        }

        final Transaction tx = graph.beginTx();
        try
        {
            graph.createNode();

            long id = -1;
            if ( config == null )
            {
                final IndexHits<Node> hits = graph.index()
                                                  .forNodes( CONFIG_NODES_IDX )
                                                  .get( CONFIG_ID, BASE_CONFIG_NODE );
                if ( hits.hasNext() )
                {
                    id = hits.next()
                             .getId();
                }
                else
                {
                }
            }

            if ( id < 0 )
            {
                if ( config == null )
                {
                    config = new GraphWorkspaceConfiguration();
                }

                configNode = graph.createNode();
                id = configNode.getId();

                Conversions.storeConfig( configNode, config );

                graph.index()
                     .forNodes( CONFIG_NODES_IDX )
                     .add( configNode, CONFIG_ID, BASE_CONFIG_NODE );

            }

            configNodeId = id;
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        globalView = new GraphView( new GraphWorkspace( "GLOBAL", this ) );
    }

    protected AbstractNeo4JEGraphDriver( final GraphDatabaseService graph, final boolean useShutdownHook )
    {
        this( null, graph, useShutdownHook );
    }

    protected GraphDatabaseService getGraph()
    {
        return graph;
    }

    protected boolean isUseShutdownHook()
    {
        return useShutdownHook;
    }

    @Override
    public void printStats()
    {
        logger.info( "Graph contains {} nodes.", graph.index()
                                                      .forNodes( BY_GAV_IDX )
                                                      .query( GAV, "*" )
                                                      .size() );

        logger.info( "Graph contains {} relationships.", graph.index()
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
        final IndexHits<Node> hits = index.get( GAV, ref.asProjectVersionRef()
                                                        .toString() );

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
        if ( registerView( view ) )
        {
            final RelationshipIndex cachedPaths = graph.index()
                                                       .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );
            final IndexHits<Relationship> hits = cachedPaths.get( CACHED_PATH_TARGETS, ref );

            final Set<Long> seen = new HashSet<Long>();
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            while ( hits.hasNext() )
            {
                final Relationship pathRel = hits.next();
                final long rid = Conversions.getLastCachedPathRelationship( pathRel );

                if ( rid > -1 && !seen.contains( rid ) )
                {
                    final Relationship r = graph.getRelationshipById( rid );
                    final ProjectRelationship<?> rel = toProjectRelationship( r );
                    seen.add( rid );

                    result.add( rel );
                }
            }

            return result;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
        final IndexHits<Node> hits = index.get( GAV, ref.asProjectVersionRef()
                                                        .toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            // FIXME: What if this view has a filter or mutator?? Without a root, that would be very strange...
            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        checkClosed();
        if ( registerView( view ) )
        {

            final Index<Node> cachedNodes = graph.index()
                                                 .forNodes( NODE_CACHE_PREFIX + view.getShortId() );

            final IndexHits<Node> nodeHits = cachedNodes.query( NID, "*" );
            final Set<ProjectVersionRef> nodes = new HashSet<ProjectVersionRef>();
            while ( nodeHits.hasNext() )
            {
                nodes.add( toProjectVersionRef( nodeHits.next() ) );
            }

            return nodes;
        }

        // FIXME: What if this view has a filter or mutator?? Without a root, that would be very strange...
        return new HashSet<ProjectVersionRef>( convertToProjects( graph.index()
                                                                       .forNodes( BY_GAV_IDX )
                                                                       .query( GAV, "*" ) ) );
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        checkClosed();
        if ( registerView( view ) )
        {

            final RelationshipIndex cachedRels = graph.index()
                                                      .forRelationships( REL_CACHE_PREFIX + view.getShortId() );

            final IndexHits<Relationship> relHits = cachedRels.query( RID, "*" );
            final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();
            while ( relHits.hasNext() )
            {
                rels.add( toProjectRelationship( relHits.next() ) );
            }

            return rels;
        }

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
                logger.debug( "Checking for self-referential parent: {}", rel );
                if ( rel.getType() == RelationshipType.PARENT && rel.getDeclaring()
                                                                    .equals( rel.getTarget()
                                                                                .asProjectVersionRef() ) )
                {
                    logger.debug( "Removing self-referential parent: {}", rel );
                    rels.remove( rel );
                }
            }

            logger.debug( "returning {} relationships: {}", rels.size(), rels );
            return rels;
        }
        else
        {
            // FIXME: What if this view has a filter or mutator?? Without a root, that would be very strange...
            final IndexHits<Relationship> hits = graph.index()
                                                      .forRelationships( ALL_RELATIONSHIPS )
                                                      .query( RELATIONSHIP_ID, "*" );
            return convertToRelationships( hits );
        }
    }

    @Override
    public Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( final GraphView view, final Set<ProjectVersionRef> refs )
    {
        checkClosed();
        if ( !registerView( view ) )
        {
            throw new IllegalArgumentException( "You must specify at least one root GAV in order to retrieve path-related info." );
        }

        final RelationshipIndex cachedPaths = graph.index()
                                                   .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );
        final IndexHits<Relationship> hits = cachedPaths.query( CACHED_PATH_TARGETS, StringUtils.join( refs, " " ) );

        final Map<GraphPath<?>, GraphPathInfo> result = new HashMap<GraphPath<?>, GraphPathInfo>();
        while ( hits.hasNext() )
        {
            final Relationship pathRel = hits.next();
            final Neo4jGraphPath path = Conversions.getCachedPath( pathRel );
            final GraphPathInfo pathInfo = Conversions.getCachedPathInfo( pathRel, this );

            result.put( path, pathInfo );
        }

        return result;
    }

    @Override
    public ProjectVersionRef getPathTargetRef( final GraphPath<?> path )
    {
        if ( path == null )
        {
            return null;
        }

        if ( !( path instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "GraphPath instances must be of type Neo4jGraphPath. Was: " + path.getClass()
                                                                                                                  .getName() );
        }

        final long rid = ( (Neo4jGraphPath) path ).getLastRelationshipId();
        final Relationship rel = graph.getRelationshipById( rid );
        final Node target = rel.getEndNode();

        return toProjectVersionRef( target );
    }

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final GraphView view, final ProjectVersionRef... refs )
    {
        checkClosed();
        if ( !registerView( view ) )
        {
            throw new IllegalArgumentException( "You must specify at least one root GAV in order to retrieve path-related info." );
        }

        final RelationshipIndex cachedPaths = graph.index()
                                                   .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );
        final IndexHits<Relationship> hits = cachedPaths.query( CACHED_PATH_TARGETS, StringUtils.join( refs, " " ) );

        final Map<Long, ProjectRelationship<?>> convertedCache = new HashMap<Long, ProjectRelationship<?>>();
        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        while ( hits.hasNext() )
        {
            final Relationship pathRel = hits.next();
            final Neo4jGraphPath path = Conversions.getCachedPath( pathRel );

            final List<ProjectRelationship<?>> convertedPath = new ArrayList<ProjectRelationship<?>>();
            for ( final Long rid : path )
            {
                ProjectRelationship<?> rel = convertedCache.get( rid );
                if ( rel == null )
                {
                    final Relationship r = graph.getRelationshipById( rid );
                    rel = toProjectRelationship( r );
                    convertedCache.put( rid, rel );

                    convertedPath.add( rel );
                }
            }

            result.add( convertedPath );
        }

        return result;

        //        // NOTE: using global lookup here to avoid checking for paths, which we're going to collect below.
        //        final Set<Node> nodes = new HashSet<Node>( refs.length );
        //        for ( final ProjectVersionRef ref : refs )
        //        {
        //            final Node n = getNode( ref );
        //            if ( n != null )
        //            {
        //                nodes.add( n );
        //            }
        //        }
        //
        //        if ( nodes.isEmpty() )
        //        {
        //            return null;
        //        }
        //
        //        final Set<Node> roots = getRoots( view );
        //        final ConnectingPathsCollector checker = new ConnectingPathsCollector( roots, nodes, view, false );
        //
        //        collectAtlasRelationships( view, checker, roots, false );
        //
        //        final Set<Path> paths = checker.getFoundPaths();
        //        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        //        for ( final Path path : paths )
        //        {
        //            result.add( convertToRelationships( path.relationships() ) );
        //        }
        //
        //        return result;
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

        final Map<ProjectRelationship<?>, Relationship> createdRelationshipsMap = new HashMap<ProjectRelationship<?>, Relationship>();
        Transaction tx = graph.beginTx();
        try
        {
            nextRel: for ( final ProjectRelationship<?> rel : rels )
            {
                logger.debug( "Checking relationship: {}", rel );

                final Index<Node> index = graph.index()
                                               .forNodes( BY_GAV_IDX );

                final ProjectVersionRef declaring = rel.getDeclaring();
                final ProjectVersionRef target = rel.getTarget()
                                                    .asProjectVersionRef();

                final Node[] nodes = new Node[2];
                int i = 0;
                for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
                {
                    final IndexHits<Node> hits = index.get( GAV, ref.asProjectVersionRef()
                                                                    .toString() );
                    if ( !hits.hasNext() )
                    {
                        logger.debug( "Creating new node for: {} to support addition of relationship: {}", ref, rel );
                        try
                        {
                            final Node node = newProjectNode( ref );
                            nodes[i] = node;
                        }
                        catch ( final InvalidVersionSpecificationException e )
                        {
                            // FIXME: This means we're discarding a rejected relationship without passing it back...NOT GOOD
                            // However, some code assumes rejects are cycles...also not good.
                            logger.error( String.format( "Failed to create node for project ref: %s. Reason: %s", ref, e.getMessage() ), e );
                            continue nextRel;
                        }
                    }
                    else
                    {
                        nodes[i] = hits.next();

                        logger.debug( "Using existing project node: {} for: {}", nodes[i], ref.asProjectVersionRef() );
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

                    if ( from.getId() != nodes[1].getId() )
                    {
                        final Node to = nodes[1];

                        logger.debug( "Creating graph relationship for: {} between node: {} and node: {}", rel, from, to );

                        final GraphRelType grt = GraphRelType.map( rel.getType(), rel.isManaged() );

                        relationship = from.createRelationshipTo( to, grt );

                        logger.debug( "New relationship is: {} with type: {}", relationship, grt );

                        toRelationshipProperties( rel, relationship );
                        relIdx.add( relationship, RELATIONSHIP_ID, relId );

                        if ( rel.isManaged() )
                        {
                            graph.index()
                                 .forRelationships( MANAGED_GA )
                                 .add( relationship, MANAGED_KEY,
                                       String.format( "%d/%s/%s:%s", relationship.getStartNode()
                                                                                 .getId(), rel.getType()
                                                                                              .name(), rel.getTarget()
                                                                                                          .getGroupId(), rel.getTarget()
                                                                                                                            .getArtifactId() ) );
                        }

                        logger.info( "+= {} ({})", relationship, toProjectRelationship( relationship ) );
                    }
                    else
                    {
                        graph.index()
                             .forNodes( MISSING_NODES_IDX )
                             .remove( from );

                        markConnected( from, true );

                        continue;
                    }

                    logger.debug( "Removing missing/incomplete flag from: {} ({})", from, declaring );
                    graph.index()
                         .forNodes( MISSING_NODES_IDX )
                         .remove( from );

                    markConnected( from, true );

                    if ( !( rel instanceof ParentRelationship ) || !( (ParentRelationship) rel ).isTerminus() )
                    {
                        createdRelationshipsMap.put( rel, relationship );
                    }
                }
                else
                {
                    relationship = relHits.next();
                    logger.debug( "== {} ({})", relationship, toProjectRelationship( relationship ) );

                    Conversions.removeProperty( Conversions.SELECTION, relationship );
                    graph.index()
                         .forRelationships( SELECTION_RELATIONSHIPS )
                         .remove( relationship, RID, relationship.getId() );

                    addToURISetProperty( rel.getSources(), SOURCE_URI, relationship );
                }
            }

            logger.debug( "Committing graph transaction." );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        final Set<ProjectRelationship<?>> skipped = new HashSet<ProjectRelationship<?>>();
        Map<ProjectRelationship<?>, Set<CyclePath>> cycleMap = null;

        tx = graph.beginTx();
        try
        {
            logger.info( "Analyzing {} for new cycles...", createdRelationshipsMap.size() );
            cycleMap = getIntroducedCycles( createdRelationshipsMap );
            skipped.addAll( cycleMap.keySet() );

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        logger.debug( "Cycle injection detected for: {}", skipped );

        logger.info( "Updating all-projects caches with {} new entries", ( rels.length - skipped.size() ) );
        updateCaches( createdRelationshipsMap, cycleMap );

        logger.info( "Returning {} rejected relationships.", skipped.size() );

        //        printGraphStats();

        return skipped;
    }

    @Override
    public boolean introducesCycle( final GraphView view, final ProjectRelationship<?> rel )
    {
        checkClosed();

        final ProjectVersionRef to = rel.getDeclaring();
        final ProjectVersionRef from = rel.getTarget()
                                          .asProjectVersionRef();

        final Node toNode = getNode( to );
        final Node fromNode = getNode( from );

        if ( toNode == null || fromNode == null )
        {
            return false;
        }

        if ( registerView( view ) )
        {
            final RelationshipIndex cachedPaths = graph.index()
                                                       .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );
            final IndexHits<Relationship> hits = cachedPaths.get( CACHED_PATH_TARGETS, toNode.getId() );
            for ( final Relationship pathRel : hits )
            {
                final Neo4jGraphPath path = Conversions.getCachedPath( pathRel );
                for ( final Long rid : path )
                {
                    final Relationship r = graph.getRelationshipById( rid );
                    if ( r.getStartNode()
                          .getId() == fromNode.getId() || r.getEndNode()
                                                           .getId() == fromNode.getId() )
                    {
                        return true;
                    }
                }
            }

            return false;
        }
        else
        {
            final ConnectingPathsCollector collector = new ConnectingPathsCollector( fromNode, toNode, view, true );
            collectAtlasRelationships( view, collector, Collections.singleton( fromNode ), false );

            return collector.hasFoundPaths();
        }
    }

    private Map<ProjectRelationship<?>, Set<CyclePath>> getIntroducedCycles( final Map<ProjectRelationship<?>, Relationship> potentialCycleInjectors )
    {
        final Map<NodePair, ProjectRelationship<?>> src = new HashMap<NodePair, ProjectRelationship<?>>();
        final Set<Node> targets = new HashSet<Node>();
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

            //            logger.info( "Listening for cycle introduced by: {}. (looking for path from: {} to: {})", rel, to.getId(), from.getId() );
            src.put( new NodePair( to, from ), rel );
        }

        if ( src.isEmpty() )
        {
            return Collections.emptyMap();
        }

        final CycleDetectingCollector checker = new CycleDetectingCollector( targets, src, globalView );
        collectAtlasRelationships( globalView, checker, targets, false );

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
        //        //            logger.warn( "\n\n\n\nCYCLE DETECTED!\n\nCycle: {}\n\n\n\n", convertToRelationships( p.relationships() ) );
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
        //            logger.warn( "\n\n\n\nCYCLE DETECTED!\n\nCycle: {}\n\n\n\n", convertToRelationships( p.relationships() ) );
        //            cycles.add( cycle );
        //        }
        //
        //        logger.info( "\n\n\n\n{} CYCLES via: {}\n\n\n\n", cycles.size(), rel );
        //        return cycles;
    }

    private Node newProjectNode( final ProjectVersionRef ref )
    {
        final Node node = graph.createNode();
        toNodeProperties( ref, node, false );

        final String gav = ref.asProjectVersionRef()
                              .toString();

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
            //            logger.info( "Adding {} to variable-nodes index.", ref );
            graph.index()
                 .forNodes( VARIABLE_NODES_IDX )
                 .add( node, GAV, gav );
        }

        //        logger.info( "Created project node: {} with id: {}", ref, node.getId() );
        return node;
    }

    public Relationship select( final Relationship old, final GraphView view, final GraphPathInfo pathInfo, final Neo4jGraphPath path )
    {
        final ProjectRelationship<?> oldRel = toProjectRelationship( old );

        //        logger.info( "Selecting mutated relationship for: {}", oldRel );
        final ProjectRelationship<?> selected = pathInfo == null ? oldRel : pathInfo.selectRelationship( oldRel, path );

        if ( selected == null )
        {
            return null;
        }

        if ( selected != oldRel )
        {
            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            //                logger.info( "Checking for existing DB relationship for: {}", selected );
            final String selId = id( selected );
            IndexHits<Relationship> hits = relIdx.get( RELATIONSHIP_ID, selId );
            if ( hits.hasNext() )
            {
                return hits.next();
            }

            logger.debug( "Creating ad-hoc db relationship for: {}", selected );
            final Set<ProjectRelationship<?>> rejected = addRelationships( selected );
            if ( rejected != null && !rejected.isEmpty() )
            {
                logger.info( "Failed to add: {}", selected );
                return null;
            }

            final Transaction tx = graph.beginTx();
            try
            {
                Relationship result = null;
                hits = relIdx.get( RELATIONSHIP_ID, selId );
                if ( hits.hasNext() )
                {
                    result = hits.next();

                    logger.debug( "Adding relatiionship {} to selections index", result );
                    result.setProperty( Conversions.SELECTION, true );

                    graph.index()
                         .forRelationships( SELECTION_RELATIONSHIPS )
                         .add( result, RID, result.getId() );
                }

                tx.success();

                return result;
            }
            finally
            {
                tx.finish();
            }
        }

        return old;
    }

    //    private Set<ProjectVersionRef> getProjectsRootedAt( final GraphView view, final Set<Node> roots )
    //    {
    //        Iterable<Node> nodes = null;
    //        if ( roots != null && !roots.isEmpty() )
    //        {
    //            final RootedNodesCollector agg = new RootedNodesCollector( roots, view, false );
    //            collectAtlasRelationships( view, agg, roots, false );
    //            nodes = agg;
    //        }
    //        else
    //        {
    //            final IndexHits<Node> hits = graph.index()
    //                                              .forNodes( BY_GAV_IDX )
    //                                              .query( GAV, "*" );
    //            nodes = hits;
    //        }
    //
    //        return new HashSet<ProjectVersionRef>( convertToProjects( nodes ) );
    //    }

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
        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            //            logger.debug( "Root node not found! (root: {})", root );
            return;
        }

        final Set<GraphRelType> relTypes = getRelTypes( traversal );

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            //            logger.debug( "PASS: {}", i );

            TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_PATH )
                                                        .sort( PathComparator.INSTANCE );

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

            //            logger.debug( "starting traverse of: {}", net );
            traversal.startTraverse( i, net );

            final Set<Long> rootIds = getRootIds( view );

            @SuppressWarnings( { "rawtypes", "unchecked" } )
            final MembershipWrappedTraversalEvaluator checker = new MembershipWrappedTraversalEvaluator( rootIds, traversal, view, i );

            description = description.expand( checker )
                                     .evaluator( checker );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                if ( path.lastRelationship() == null )
                {
                    continue;
                }

                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships() );
                logger.debug( "traversing path: {}", rels );
                for ( final ProjectRelationship<?> rel : rels )
                {
                    logger.debug( "traverse: {}", rel );
                    if ( traversal.traverseEdge( rel, rels, i ) )
                    {
                        logger.debug( "traversed: {}", rel );
                        traversal.edgeTraversed( rel, rels, i );
                    }
                }
            }

            traversal.endTraverse( i, net );
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

    // FIXME: Find a way to track this incrementally for a view...it's incredibly expensive as-is
    // possibly, have views register themselves for such tracking??
    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        final IndexHits<Node> missing = graph.index()
                                             .forNodes( MISSING_NODES_IDX )
                                             .get( GAV, ref.asProjectVersionRef()
                                                           .toString() );
        if ( missing.size() > 0 )
        {
            return false;
        }

        //        if ( view != null )
        //        {
        //            return getAllProjects( view ).contains( ref );
        //        }

        return getNode( view, ref ) != null;
    }

    // FIXME: Find a way to track this incrementally for a view...it's incredibly expensive as-is
    // FIXME: This currently doesn't seem to check that the relationship is within the filter/mutator/root config for the view...
    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        return getRelationship( rel ) != null;
    }

    protected Node getNode( final ProjectVersionRef ref )
    {
        return getNode( null, ref );
    }

    protected Node getNode( final GraphView view, final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( BY_GAV_IDX );

        final IndexHits<Node> hits = idx.get( GAV, ref.asProjectVersionRef()
                                                      .toString() );

        if ( hits.size() < 1 )
        {
            return null;
        }

        final Node node = hits.next();

        //        logger.debug( "Query result for node: {} is: {}\nChecking for path to root(s): {}", ref, node,
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

        GraphView v = view;
        if ( v == null )
        {
            v = globalView;
        }

        final Set<Node> roots = getRoots( v );
        if ( roots == null || roots.isEmpty() )
        {
            return true;
        }

        if ( roots.contains( node ) )
        {
            logger.debug( "Node {} IS a root of {}", node, view.getLongId() );
            return true;
        }

        logger.debug( "Checking for path between roots: {} and target node: {}", new JoinString( ",", roots ), node.getId() );
        final EndNodesCollector checker = new EndNodesCollector( roots, Collections.singleton( node ), view, false );

        collectAtlasRelationships( v, checker, roots, false );
        return checker.hasFoundNodes();
    }

    protected Relationship getRelationship( final ProjectRelationship<?> rel )
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

            if ( graph.index()
                      .existsForRelationships( SELECTION_RELATIONSHIPS ) )
            {
                final Transaction tx = graph.beginTx();
                try
                {
                    final RelationshipIndex idx = graph.index()
                                                       .forRelationships( SELECTION_RELATIONSHIPS );

                    final IndexHits<Relationship> hits = idx.get( RID, "*" );
                    for ( final Relationship r : hits )
                    {
                        if ( r.hasProperty( Conversions.SELECTION ) )
                        {
                            r.delete();
                        }
                    }

                    graph.index()
                         .forRelationships( SELECTION_RELATIONSHIPS )
                         .delete();

                    tx.success();
                }
                finally
                {
                    tx.finish();
                }
            }

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
            //            new Logger( getClass() ).debug( "Failed to shutdown graph database. Reason: {}", e, e.getMessage() );
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
                                          .forNodes( MISSING_NODES_IDX )
                                          .get( GAV, ref.asProjectVersionRef()
                                                        .toString() );

        return hits.size() > 0;
        //        final IndexHits<Node> hits = graph.index()
        //                                          .forNodes( BY_GAV_IDX )
        //                                          .get( GAV, ref.asProjectVersionRef().toString() );
        //
        //        if ( hits.size() > 0 )
        //        {
        //            return !isConnected( hits.next() );
        //        }
        //
        //        return false;
    }

    @Override
    public boolean hasMissingProjects( final GraphView view )
    {
        return hasIndexedProjects( view, MISSING_NODES_IDX );
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects( final GraphView view )
    {
        return getIndexedProjects( view, MISSING_NODES_IDX );
    }

    private Set<ProjectVersionRef> getIndexedProjects( final GraphView view, final String indexName )
    {
        checkClosed();

        final IndexHits<Node> hits = graph.index()
                                          .forNodes( indexName )
                                          .query( GAV, "*" );

        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();

        if ( registerView( view ) )
        {
            final Index<Node> cachedNodes = graph.index()
                                                 .forNodes( NODE_CACHE_PREFIX + view.getShortId() );

            for ( final Node node : hits )
            {
                final IndexHits<Node> cacheHits = cachedNodes.get( NID, node.getId() );
                if ( cacheHits.hasNext() )
                {
                    result.add( toProjectVersionRef( node ) );
                }
            }
        }
        else
        {
            for ( final Node node : hits )
            {
                result.add( toProjectVersionRef( node ) );
            }
        }

        return result;
    }

    private boolean hasIndexedProjects( final GraphView view, final String indexName )
    {
        checkClosed();

        final IndexHits<Node> hits = graph.index()
                                          .forNodes( indexName )
                                          .query( GAV, "*" );

        if ( registerView( view ) )
        {
            final Index<Node> cachedNodes = graph.index()
                                                 .forNodes( NODE_CACHE_PREFIX + view.getShortId() );

            for ( final Node node : hits )
            {
                final IndexHits<Node> cacheHits = cachedNodes.get( NID, node.getId() );
                if ( cacheHits.hasNext() )
                {
                    return true;
                }
            }
        }
        else
        {
            if ( hits.hasNext() )
            {
                return true;
            }
        }

        return false;
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

        //        logger.info( "Traversing for aggregation using: {} from roots: {}", checker.getClass()
        //                                                                                   .getName(), from );

        TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_PATH );
        if ( sorted )
        {
            description = description.sort( PathComparator.INSTANCE );
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
            //            logger.info( "Aggregating path: {}", path );
            // Don't need this, but we need to iterate the traverser.
        }
    }

    @Override
    public boolean hasVariableProjects( final GraphView view )
    {
        return hasIndexedProjects( view, VARIABLE_NODES_IDX );
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final GraphView view )
    {
        return getIndexedProjects( view, VARIABLE_NODES_IDX );
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
        checkClosed();

        RelationshipIndex cycleIdx;
        if ( registerView( view ) )
        {
            logger.debug( "Getting cycles for view: {}", view.getShortId() );
            cycleIdx = graph.index()
                            .forRelationships( CYCLE_CACHE_PREFIX + view.getShortId() );
        }
        else
        {
            logger.debug( "Getting ALL cycles" );
            cycleIdx = graph.index()
                            .forRelationships( ALL_CYCLES );
        }

        final IndexHits<Relationship> hits = cycleIdx.query( RID, "*" );

        //        final IndexHits<Relationship> hits = graph.index()
        //                                                  .forRelationships( CYCLE_INJECTION_IDX )
        //                                                  .query( RELATIONSHIP_ID, "*" );
        //
        //        final Map<Node, Relationship> targetNodes = new HashMap<Node, Relationship>();
        //        for ( final Relationship hit : hits )
        //        {
        //            targetNodes.put( hit.getStartNode(), hit );
        //        }
        //
        //        final Set<Path> paths = getPathsTo( view, targetNodes.keySet() );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        final ConversionCache cache = new ConversionCache();
        for ( final Relationship cycleRel : hits )
        {
            final Neo4jGraphPath cyclicPath = Conversions.getCachedPath( cycleRel );
            final long injectorId = Conversions.getLongProperty( RID, cycleRel, -1 );
            if ( injectorId < 0 )
            {
                logger.error( "Detected improperly stored cycle! Injector relationship-id is missing in {} with cyclic path: {}", cycleRel.getId(),
                              cyclicPath );
                continue;
            }

            final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>( cyclicPath.length() + 1 );
            for ( final long id : cyclicPath.getRelationshipIds() )
            {
                ProjectRelationship<?> rel = cache.getRelationship( id );
                if ( rel == null )
                {
                    final Relationship r = graph.getRelationshipById( id );
                    rel = toProjectRelationship( r, cache );
                }
                cycle.add( rel );
            }

            ProjectRelationship<?> rel = cache.getRelationship( injectorId );
            if ( rel == null )
            {
                final Relationship injector = graph.getRelationshipById( injectorId );
                rel = toProjectRelationship( injector, cache );
            }

            cycle.add( rel );

            cycles.add( new EProjectCycle( cycle ) );
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
    public Map<String, String> getMetadata( final ProjectVersionRef ref, final Set<String> keys )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return null;
        }

        return getMetadataMap( node, keys );
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
            throw new GraphDriverException( "Leave off the START clause when supplying ProjectVersionRef instances as query roots:\n'{}'", cypher );
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
            throw new GraphDriverException( "Leave off the START clause when supplying ProjectRelationship instances as query roots:\n'{}'", cypher );
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

        logger.debug( "Running query:\n\n{}\n\nWith params:\n\n{}\n\n", cypher, params );

        final String query = cypher.replaceAll( "(\\s)\\s+", "$1" );

        final ExecutionResult result = params == null ? queryEngine.execute( query ) : queryEngine.execute( query, params );

        //        logger.info( "Execution plan:\n{}", result.executionPlanDescription() );

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
            final GraphView view = globalView;
            final Iterable<Node> nodes = getAllProjectNodes( view );
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
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !containsProject( null, ref ) )
        {
            synchronized ( this )
            {
                final Transaction tx = graph.beginTx();
                try
                {
                    logger.debug( "Creating new node to account for disconnected project: {}", ref );
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

    /**
     * @deprecated Use {@link #getDirectRelationshipsFrom(GraphView,ProjectVersionRef,boolean,boolean,RelationshipType...)} instead
     */
    @Deprecated
    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo, final RelationshipType... types )
    {
        return getDirectRelationshipsFrom( view, from, includeManagedInfo, true, types );
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo, final boolean includeConcreteInfo,
                                                                   final RelationshipType... types )
    {
        final Node node = getNode( from );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            if ( includeConcreteInfo )
            {
                grts.add( GraphRelType.map( relType, false ) );
            }

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

    /**
     * @deprecated Use {@link #getDirectRelationshipsTo(GraphView,ProjectVersionRef,boolean,boolean,RelationshipType...)} instead
     */
    @Deprecated
    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        return getDirectRelationshipsTo( view, to, includeManagedInfo, true, types );
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                 final boolean includeConcreteInfo, final RelationshipType... types )
    {
        logger.debug( "Finding relationships targeting: {} (filter: {}, managed: {}, types: {})", to, view.getFilter(), includeManagedInfo,
                      Arrays.asList( types ) );
        final Node node = getNode( to );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            if ( includeConcreteInfo )
            {
                grts.add( GraphRelType.map( relType, false ) );
            }

            if ( includeManagedInfo )
            {
                grts.add( GraphRelType.map( relType, true ) );
            }
        }

        logger.debug( "Using graph-relationship types: {}", grts );

        final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING, grts.toArray( new GraphRelType[grts.size()] ) );

        if ( relationships != null )
        {
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            for ( final Relationship r : relationships )
            {
                logger.debug( "Examining relationship: {}", r );
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
                                          .query( GA, projectRef.asProjectRef()
                                                                .toString() );
        return new HashSet<ProjectVersionRef>( convertToProjects( hits ) );
    }

    @Override
    public void deleteRelationshipsDeclaredBy( final ProjectVersionRef ref )
        throws GraphDriverException
    {
        checkClosed();

        if ( ref == null )
        {
            return;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );

        final String gav = ref.asProjectVersionRef()
                              .toString();

        final IndexHits<Node> hits = index.get( GAV, gav );

        if ( hits.hasNext() )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                final Node node = hits.next();
                final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING );
                if ( relationships != null )
                {
                    for ( final Relationship r : relationships )
                    {
                        r.delete();
                    }
                }

                graph.index()
                     .forNodes( MISSING_NODES_IDX )
                     .add( node, GAV, gav );

                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
    }

    @Override
    public ProjectVersionRef getManagedTargetFor( final ProjectVersionRef target, final GraphPath<?> path, final RelationshipType type )
    {
        if ( path == null )
        {
            return null;
        }

        if ( !( path instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "GraphPath instances must be of type Neo4jGraphPath. Was: " + path.getClass()
                                                                                                                  .getName() );
        }

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( MANAGED_GA );

        //        logger.info( "Searching for managed override of: {} in: {}", target, path );
        final Neo4jGraphPath neopath = (Neo4jGraphPath) path;
        for ( final Long id : neopath )
        {
            final Relationship r = graph.getRelationshipById( id );

            final String mkey = String.format( "%d/%s/%s:%s", r.getStartNode()
                                                               .getId(), type.name(), target.getGroupId(), target.getArtifactId() );

            //            logger.info( "Searching for m-key: {}", mkey );

            final IndexHits<Relationship> hits = idx.get( MANAGED_KEY, mkey );
            if ( hits != null && hits.hasNext() )
            {
                final ProjectVersionRef ref = toProjectVersionRef( hits.next()
                                                                       .getEndNode() );

                //                logger.info( "Found it: {}", ref );
                return ref;
            }

            //            final Node node = graph.getNodeById( id );
            //            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING, GraphRelType.map( type, true ) );
            //            if ( relationships != null )
            //            {
            //                for ( final Relationship r : relationships )
            //                {
            //                    if ( r.hasProperty( GROUP_ID ) && r.getProperty( GROUP_ID )
            //                                                       .equals( target.getGroupId() ) && r.hasProperty( ARTIFACT_ID )
            //                        && r.getProperty( ARTIFACT_ID )
            //                            .equals( target.getArtifactId() ) )
            //                    {
            //                        return toProjectVersionRef( r.getEndNode() );
            //                    }
            //                }
            //            }
        }

        return null;
    }

    @Override
    public GraphPath<?> createPath( final ProjectRelationship<?>... rels )
    {
        final long[] relIds = new long[rels.length];
        for ( int i = 0; i < rels.length; i++ )
        {
            final Relationship r = getRelationship( rels[i] );
            relIds[i] = r.getId();
        }

        return new Neo4jGraphPath( relIds );
    }

    @Override
    public GraphPath<?> createPath( final GraphPath<?> parent, final ProjectRelationship<?> rel )
    {
        if ( parent != null && !( parent instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "Cannot get child path-key for: " + parent + ". This is not a Neo4jGraphPathKey instance!" );
        }

        Relationship r = getRelationship( rel );
        if ( r == null )
        {
            synchronized ( this )
            {
                final Transaction tx = graph.beginTx();
                try
                {
                    logger.debug( "Creating new node to account for missing project referenced in path: {}", r );
                    final Set<ProjectRelationship<?>> rejected = addRelationships( rel );
                    if ( rejected != null && !rejected.isEmpty() )
                    {
                        throw new IllegalArgumentException( "Cannot create missing relationship for: " + rel + ". It creates a relationship cycle." );
                    }

                    r = getRelationship( rel );

                    tx.success();
                }
                finally
                {
                    tx.finish();
                }
            }
        }

        return new Neo4jGraphPath( (Neo4jGraphPath) parent, r.getId() );
    }

    @Override
    public void setLastAccess( final long lastAccess )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );
            node.setProperty( Conversions.LAST_ACCESS, lastAccess );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public long getLastAccess()
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.getLongProperty( Conversions.LAST_ACCESS, node, -1 );
    }

    @Override
    public int getActivePomLocationCount()
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.countArrayElements( Conversions.ACTIVE_POM_LOCATIONS, node );
    }

    @Override
    public void addActivePomLocations( final URI... locations )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.addToURISetProperty( Arrays.asList( locations ), Conversions.ACTIVE_POM_LOCATIONS, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void addActivePomLocations( final Collection<URI> locations )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.addToURISetProperty( locations, Conversions.ACTIVE_POM_LOCATIONS, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void removeActivePomLocations( final URI... locations )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.removeFromURISetProperty( Arrays.asList( locations ), Conversions.ACTIVE_POM_LOCATIONS, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void removeActivePomLocations( final Collection<URI> locations )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.removeFromURISetProperty( locations, Conversions.ACTIVE_POM_LOCATIONS, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public Set<URI> getActivePomLocations()
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.getURISetProperty( Conversions.ACTIVE_POM_LOCATIONS, node, null );
    }

    @Override
    public int getActiveSourceCount()
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.countArrayElements( Conversions.ACTIVE_SOURCES, node );
    }

    @Override
    public void addActiveSources( final Collection<URI> sources )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.addToURISetProperty( sources, Conversions.ACTIVE_SOURCES, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void addActiveSources( final URI... sources )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.addToURISetProperty( Arrays.asList( sources ), Conversions.ACTIVE_SOURCES, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public Set<URI> getActiveSources()
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.getURISetProperty( Conversions.ACTIVE_SOURCES, node, null );
    }

    @Override
    public void removeActiveSources( final URI... sources )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.removeFromURISetProperty( Arrays.asList( sources ), Conversions.ACTIVE_SOURCES, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void removeActiveSources( final Collection<URI> sources )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            Conversions.removeFromURISetProperty( sources, Conversions.ACTIVE_SOURCES, node );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public String setProperty( final String key, final String value )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );
            final String result = Conversions.setConfigProperty( key, value, node );

            tx.success();

            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public String removeProperty( final String key )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = graph.getNodeById( configNodeId );

            final String result = Conversions.removeConfigProperty( key, node );
            tx.success();

            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public String getProperty( final String key )
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.getConfigProperty( key, node, null );
    }

    @Override
    public String getProperty( final String key, final String defaultVal )
    {
        final Node node = graph.getNodeById( configNodeId );
        return Conversions.getConfigProperty( key, node, defaultVal );
    }

    @Override
    public boolean registerView( final GraphView view )
    {
        if ( view.getRoots() == null || view.getRoots()
                                            .isEmpty() )
        {
            logger.info( "Cannot track membership in view! It has no root GAVs." );
            return false;
        }

        final Index<Node> confIdx = graph.index()
                                         .forNodes( CONFIG_NODES_IDX );

        final IndexHits<Node> hits = confIdx.get( VIEW_ID, view.getShortId() );
        if ( !hits.hasNext() )
        {
            final ConversionCache cache = new ConversionCache();

            final Transaction tx = graph.beginTx();
            try
            {
                final Node viewNode = graph.createNode();
                Conversions.storeView( view, viewNode );

                confIdx.add( viewNode, VIEW_ID, view.getShortId() );

                final RelationshipIndex cachedPathRels = graph.index()
                                                              .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );

                final RelationshipIndex cachedRels = graph.index()
                                                          .forRelationships( REL_CACHE_PREFIX + view.getShortId() );

                final Index<Node> cachedNodes = graph.index()
                                                     .forNodes( NODE_CACHE_PREFIX + view.getShortId() );

                final Set<Node> roots = cacheRoots( view, viewNode, cachedPathRels, cachedNodes );
                if ( roots.isEmpty() )
                {
                    logger.info( "Cannot track membership in view! It has no root GAVs." );
                    return false;
                }

                logger.info( "Registering new view: {}", view.getLongId() );

                final RootedPathsCollector checker = new RootedPathsCollector( roots, view );

                collectAtlasRelationships( view, checker, roots, false );

                for ( final Entry<Neo4jGraphPath, GraphPathInfo> entry : checker.getPathInfoMap()
                                                                                .entrySet() )
                {
                    final Neo4jGraphPath path = entry.getKey();
                    final GraphPathInfo pathInfo = entry.getValue();

                    cachePath( path, pathInfo, viewNode, cachedPathRels, cachedRels, cachedNodes );
                }

                final RelationshipIndex cachedCycleRels = graph.index()
                                                               .forRelationships( CYCLE_CACHE_PREFIX + view.getShortId() );

                final IndexHits<Relationship> allCycleHits = graph.index()
                                                                  .forRelationships( ALL_CYCLES )
                                                                  .query( RID, "*" );

                final Map<ProjectRelationship<?>, Set<CyclePath>> cycleMap = new HashMap<ProjectRelationship<?>, Set<CyclePath>>();
                for ( final Relationship cycleHit : allCycleHits )
                {
                    final CyclePath path = Conversions.getCachedCyclePath( cycleHit );
                    logger.debug( "Found cycle path: {} in path relationship: {}", path, cycleHit );

                    final long injectorId = Conversions.getLongProperty( RID, cycleHit, -1 );

                    if ( injectorId < 0 )
                    {
                        logger.error( "Detected improperly stored cycle! Injector relationship-id is missing in {} with cyclic path: {}",
                                      cycleHit.getId(), path );
                        continue;
                    }

                    if ( cachedCycleRels.get( RID, injectorId )
                                        .hasNext() )
                    {
                        // already cached; skip it.
                        continue;
                    }

                    ProjectRelationship<?> injectorRel = cache.getRelationship( injectorId );
                    if ( injectorRel == null )
                    {
                        final Relationship injector = graph.getRelationshipById( injectorId );
                        injectorRel = toProjectRelationship( injector, cache );
                    }

                    Set<CyclePath> paths = cycleMap.get( injectorId );
                    if ( paths == null )
                    {
                        paths = new HashSet<CyclePath>();
                        cycleMap.put( injectorRel, paths );
                    }

                    paths.add( path );
                }

                int cycles = 0;
                final Set<CyclePath> seenCycles = new HashSet<CyclePath>();
                for ( final Entry<ProjectRelationship<?>, Set<CyclePath>> entry : cycleMap.entrySet() )
                {
                    final ProjectRelationship<?> injectorRel = entry.getKey();
                    final Relationship injector = getRelationship( injectorRel );
                    final Set<CyclePath> paths = entry.getValue();

                    for ( final CyclePath path : paths )
                    {
                        if ( cacheCycle( path, injector, cachedCycleRels, cachedPathRels, cachedRels, cycleMap, view, viewNode, seenCycles, cache ) )
                        {
                            cycles++;
                        }
                    }
                }

                logger.info( "Registered {} cycles in view {}'s cycle cache.", cycles, view.getShortId() );

                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }

        return true;
    }

    @Override
    public void registerViewSelection( final GraphView view, final ProjectRef ref, final ProjectVersionRef projectVersionRef )
    {
        checkClosed();
        if ( !registerView( view ) )
        {
            return;
        }

        IndexHits<Node> nodeHits;
        if ( ref instanceof ProjectVersionRef )
        {
            nodeHits = graph.index()
                            .forNodes( BY_GAV_IDX )
                            .get( GAV, ( (ProjectVersionRef) ref ).asProjectVersionRef()
                                                                  .toString() );
        }
        else
        {
            nodeHits = graph.index()
                            .forNodes( BY_GA_IDX )
                            .get( GA, ref.asProjectRef() );
        }

        final RelationshipIndex cachedRels = graph.index()
                                                  .forRelationships( REL_CACHE_PREFIX + view.getShortId() );

        final Index<Node> cachedNodes = graph.index()
                                             .forNodes( NODE_CACHE_PREFIX + view.getShortId() );

        final RelationshipIndex cachedPathRels = graph.index()
                                                      .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );

        final ConversionCache cache = new ConversionCache();
        final Map<Neo4jGraphPath, GraphPathInfo> toExtend = new HashMap<Neo4jGraphPath, GraphPathInfo>();
        final Set<Node> toExtendRoots = new HashSet<Node>();

        final Set<Node> toUncacheNode = new HashSet<Node>();
        final Set<Relationship> toUncache = new HashSet<Relationship>();
        final Set<Relationship> toUncachePath = new HashSet<Relationship>();

        final Transaction tx = graph.beginTx();
        try
        {
            for ( final Node node : nodeHits )
            {
                toUncacheNode.add( node );

                final IndexHits<Relationship> cachedHits = cachedPathRels.get( CACHED_PATH_CONTAINS_NODE, node.getId() );

                for ( final Relationship pathRel : cachedHits )
                {
                    toUncachePath.add( pathRel );

                    final Neo4jGraphPath path = Conversions.getCachedPath( pathRel );

                    Neo4jGraphPath newPath = new Neo4jGraphPath();
                    GraphPathInfo newPathInfo = new GraphPathInfo( view );

                    Node extendRoot = null;
                    boolean uncache = false;
                    for ( final Long id : path )
                    {
                        final Relationship r = graph.getRelationshipById( id );

                        if ( !uncache && r.getStartNode()
                                          .getId() != r.getEndNode()
                                                       .getId() )
                        {
                            if ( r.getStartNode()
                                  .getId() == node.getId() )
                            {
                                logger.debug( "Uncaching subgraph from root: {}. This will NOT be replaced automatically...", r.getEndNode() );
                                toUncacheNode.add( r.getEndNode() );
                                logger.debug( "Uncaching: {}", r );
                                toUncache.add( r );
                                uncache = true;
                                continue;
                            }
                            else if ( r.getEndNode()
                                       .getId() == node.getId() )
                            {
                                extendRoot = r.getStartNode();
                                logger.debug( "Uncaching subgraph: {}", r.getEndNode() );
                                logger.debug( "Uncaching: {}", r );
                                toUncache.add( r );
                                uncache = true;
                                continue;
                            }
                        }
                        else if ( uncache )
                        {
                            logger.debug( "Uncaching: {}", r );
                            toUncacheNode.add( r.getStartNode() );
                            toUncacheNode.add( r.getEndNode() );
                            toUncache.add( r );
                            continue;
                        }

                        final Relationship selected = select( r, view, newPathInfo, newPath );
                        if ( selected != null )
                        {
                            final ProjectRelationship<?> rel = toProjectRelationship( pathRel, cache );
                            extendRoot = r.getEndNode();
                            newPath = new Neo4jGraphPath( newPath, r.getId() );
                            newPathInfo = newPathInfo == null ? null : newPathInfo.getChildPathInfo( rel );
                        }
                    }

                    if ( extendRoot != null )
                    {
                        toExtend.put( newPath, newPathInfo );
                        toExtendRoots.add( extendRoot );
                    }
                    else
                    {
                        // TODO: Is this true??
                        logger.debug( "Whole path from root removed. Cannot rebuild without fixing view roots." );
                    }
                }
            }

            for ( final Node uncache : toUncacheNode )
            {
                logger.debug( "Uncache: {}", uncache );
                cachedNodes.remove( uncache );
            }

            for ( final Relationship uncache : toUncachePath )
            {
                logger.debug( "Uncache: {}", uncache );
                cachedPathRels.remove( uncache );
            }

            for ( final Relationship uncache : toUncache )
            {
                logger.debug( "Uncache: {}", uncache );
                cachedRels.remove( uncache );
            }

            final Index<Node> confIdx = graph.index()
                                             .forNodes( CONFIG_NODES_IDX );

            final IndexHits<Node> hits = confIdx.get( VIEW_ID, view.getShortId() );
            final Node viewNode = hits.next();

            logger.debug( "Extending through selections for: {} (pathMap: {})", toExtendRoots, toExtend );

            extendCachedPaths( toExtend, toExtendRoots, view, viewNode, cachedPathRels, cachedRels, cachedNodes );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private void cachePath( final Neo4jGraphPath path, final GraphPathInfo pathInfo, final Node viewNode, final RelationshipIndex cachePathRels,
                            final RelationshipIndex cachedRels, final Index<Node> cachedNodes )
    {
        final String key = path.getKey();
        final IndexHits<Relationship> pathRelHits = cachePathRels.get( CACHED_PATH_RELATIONSHIP, key );

        if ( pathRelHits.hasNext() )
        {
            // already cached.
            return;
        }

        logger.info( "Extending membership of view: {} with path: {}", viewNode, path );

        final long lastRelId = path.getLastRelationshipId();
        final Relationship lastRel = graph.getRelationshipById( lastRelId );
        final Node targetNode = lastRel.getEndNode();

        final Relationship pathsRel = viewNode.createRelationshipTo( targetNode, GraphRelType.CACHED_PATH_RELATIONSHIP );
        Conversions.storeCachedPath( path, pathInfo, pathsRel );

        cachePathRels.add( pathsRel, CACHED_PATH_RELATIONSHIP, key );
        cachePathRels.add( pathsRel, CACHED_PATH_TARGETS, targetNode.getId() );

        final Set<Long> nodes = new HashSet<Long>();
        for ( final Long relId : path )
        {
            final Relationship r = graph.getRelationshipById( relId );

            cachePathRels.add( pathsRel, CACHED_PATH_CONTAINS_REL, relId );
            cachedRels.add( r, RID, relId );

            final long startId = r.getStartNode()
                                  .getId();
            if ( nodes.add( startId ) )
            {
                cachePathRels.add( pathsRel, CACHED_PATH_CONTAINS_NODE, startId );
                cachedNodes.add( r.getStartNode(), NID, startId );
            }

            final long endId = r.getEndNode()
                                .getId();
            if ( nodes.add( endId ) )
            {
                cachePathRels.add( pathsRel, CACHED_PATH_CONTAINS_NODE, endId );
                cachedNodes.add( r.getEndNode(), NID, endId );
            }
        }
    }

    private Set<Node> cacheRoots( final GraphView view, final Node viewNode, final Index<Relationship> cachePathRels, final Index<Node> cachedNodes )
    {
        final Set<ProjectVersionRef> roots = view.getRoots();
        if ( roots == null || roots.isEmpty() )
        {
            return Collections.emptySet();
        }

        final Neo4jGraphPath rootPath = new Neo4jGraphPath();
        final String rootPathKey = rootPath.getKey();

        final GraphPathInfo rootPathInfo = new GraphPathInfo( view );

        final Set<Node> rootNodes = new HashSet<Node>();
        for ( final ProjectVersionRef root : roots )
        {
            Node node = getNode( root );
            if ( node == null )
            {
                node = newProjectNode( root );
            }

            rootNodes.add( node );

            final long nid = node.getId();

            final Relationship pathsRel = viewNode.createRelationshipTo( node, GraphRelType.CACHED_PATH_RELATIONSHIP );
            Conversions.storeCachedPath( rootPath, rootPathInfo, pathsRel );

            cachePathRels.add( pathsRel, CACHED_PATH_RELATIONSHIP, rootPathKey );
            cachePathRels.add( pathsRel, CACHED_PATH_TARGETS, nid );
            cachePathRels.add( pathsRel, CACHED_PATH_CONTAINS_NODE, nid );
            cachedNodes.add( node, NID, nid );
        }

        return rootNodes;
    }

    private void updateCaches( final Map<ProjectRelationship<?>, Relationship> createdRelationshipsMap,
                               final Map<ProjectRelationship<?>, Set<CyclePath>> cycleMap )
    {
        if ( createdRelationshipsMap.isEmpty() && cycleMap.isEmpty() )
        {
            return;
        }

        final Index<Node> confIdx = graph.index()
                                         .forNodes( CONFIG_NODES_IDX );

        final IndexHits<Node> hits = confIdx.query( VIEW_ID, "*" );

        final ConversionCache cache = new ConversionCache();

        for ( final Node viewNode : hits )
        {
            if ( !updateViewCaches( viewNode, createdRelationshipsMap, cycleMap, cache ) )
            {
                confIdx.remove( viewNode );
                viewNode.delete();
            }
        }

        final Transaction tx = graph.beginTx();
        try
        {
            if ( cycleMap != null && !cycleMap.isEmpty() )
            {
                final RelationshipIndex cyclePathRels = graph.index()
                                                             .forRelationships( ALL_CYCLES );

                int cycles = 0;
                final Set<CyclePath> seenCycles = new HashSet<CyclePath>();
                for ( final Entry<ProjectRelationship<?>, Set<CyclePath>> entry : cycleMap.entrySet() )
                {
                    final ProjectRelationship<?> injectingRel = entry.getKey();
                    final Relationship injector = createdRelationshipsMap.get( injectingRel );
                    injector.setProperty( Conversions.CYCLES_INJECTED, true );

                    final Set<CyclePath> cyclicPaths = entry.getValue();
                    for ( final CyclePath cyclicPath : cyclicPaths )
                    {
                        if ( cacheCycle( cyclicPath, injector, cyclePathRels, null, null, cycleMap, globalView, configNode, seenCycles, cache ) )
                        {
                            cycles++;
                        }
                    }
                }

                logger.info( "Updated global cycle cache with {} new cycles.", cycles );
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

    }

    private boolean updateViewCaches( final Node viewNode, final Map<ProjectRelationship<?>, Relationship> createdRelationshipsMap,
                                      final Map<ProjectRelationship<?>, Set<CyclePath>> cycleMap, final ConversionCache cache )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final GraphView view = Conversions.retrieveView( viewNode, this );
            if ( view == null )
            {
                return false;
            }

            final RelationshipIndex cachedPathRels = graph.index()
                                                          .forRelationships( PATH_CACHE_PREFIX + view.getShortId() );

            final RelationshipIndex cachedRels = graph.index()
                                                      .forRelationships( REL_CACHE_PREFIX + view.getShortId() );

            final Index<Node> cachedNodes = graph.index()
                                                 .forNodes( NODE_CACHE_PREFIX + view.getShortId() );

            if ( !createdRelationshipsMap.isEmpty() )
            {
                final Map<Neo4jGraphPath, GraphPathInfo> toExtend = new HashMap<Neo4jGraphPath, GraphPathInfo>();
                final Set<Node> toExtendRoots = new HashSet<Node>();

                for ( final Entry<ProjectRelationship<?>, Relationship> entry : createdRelationshipsMap.entrySet() )
                {
                    final ProjectRelationship<?> rel = entry.getKey();
                    if ( cycleMap != null && cycleMap.containsKey( rel ) )
                    {
                        continue;
                    }

                    final Relationship add = entry.getValue();

                    final Node start = add.getStartNode();
                    final IndexHits<Relationship> pathsRelHits = cachedPathRels.get( CACHED_PATH_TARGETS, start.getId() );
                    for ( final Relationship pathRel : pathsRelHits )
                    {
                        Neo4jGraphPath path = Conversions.getCachedPath( pathRel );
                        GraphPathInfo pathInfo = Conversions.getCachedPathInfo( pathRel, this );

                        final Relationship real = select( add, view, pathInfo, path );

                        if ( real != null )
                        {
                            final ProjectRelationship<?> realRel = toProjectRelationship( real, cache );

                            pathInfo = pathInfo.getChildPathInfo( realRel );
                            path = new Neo4jGraphPath( path, real.getId() );

                            cachePath( path, pathInfo, viewNode, cachedPathRels, cachedRels, cachedNodes );

                            toExtend.put( path, pathInfo );
                            toExtendRoots.add( real.getEndNode() );
                        }
                    }
                }

                extendCachedPaths( toExtend, toExtendRoots, view, viewNode, cachedPathRels, cachedRels, cachedNodes );
            }

            if ( cycleMap != null && !cycleMap.isEmpty() )
            {
                final RelationshipIndex cyclePathRels = graph.index()
                                                             .forRelationships( CYCLE_CACHE_PREFIX + view.getShortId() );

                int cycles = 0;
                final Set<CyclePath> seenCycles = new HashSet<CyclePath>();
                for ( final Entry<ProjectRelationship<?>, Set<CyclePath>> entry : cycleMap.entrySet() )
                {
                    final ProjectRelationship<?> injectingRel = entry.getKey();
                    final Relationship injector = createdRelationshipsMap.get( injectingRel );

                    final Set<CyclePath> cyclicPaths = entry.getValue();
                    for ( final CyclePath cyclicPath : cyclicPaths )
                    {
                        if ( cacheCycle( cyclicPath, injector, cyclePathRels, cachedPathRels, cachedRels, cycleMap, view, viewNode, seenCycles, cache ) )
                        {
                            cycles++;
                        }
                    }
                }

                logger.info( "Updated view {}'s cycle cache with {} new cycles.", view.getLongId(), cycles );
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return true;
    }

    private void extendCachedPaths( final Map<Neo4jGraphPath, GraphPathInfo> toExtend, final Set<Node> toExtendRoots, final GraphView view,
                                    final Node viewNode, final RelationshipIndex cachedPathRels, final RelationshipIndex cachedRels,
                                    final Index<Node> cachedNodes )
    {
        // this part is expensive...we're using the furthest-reach node from 
        // the paths we just extended as roots for further extension, and 
        // using a graph traverse to perform that extension
        //
        // Doing this eagerly (not lazily) imposes an unnecessary penalty on 
        // users who won't ever call getAll{Relationships,Projects,PathsTo}()
        // but without it the cache is basically useless (not comprehensive,
        // so it must be augmented by a traverse during those calls, to be sure).
        if ( !toExtend.isEmpty() )
        {
            final RootedPathsCollector checker = new RootedPathsCollector( toExtendRoots, toExtend, view );

            collectAtlasRelationships( view, checker, toExtendRoots, false );

            for ( final Entry<Neo4jGraphPath, GraphPathInfo> entry : checker.getPathInfoMap()
                                                                            .entrySet() )
            {
                final Neo4jGraphPath path = entry.getKey();
                final GraphPathInfo pathInfo = entry.getValue();

                cachePath( path, pathInfo, viewNode, cachedPathRels, cachedRels, cachedNodes );
            }
        }
    }

    private boolean cacheCycle( final CyclePath cyclicPath, final Relationship injector, final RelationshipIndex cyclePathRels,
                                final RelationshipIndex cachedPathRels, final RelationshipIndex cachedRels,
                                final Map<ProjectRelationship<?>, Set<CyclePath>> cycleMap, final GraphView view, final Node viewNode,
                                final Set<CyclePath> seenCycles, final ConversionCache cache )
    {
        if ( !seenCycles.add( cyclicPath ) )
        {
            logger.debug( "Already seen cycle path: {}", cyclicPath );
            return false;
        }

        if ( cyclicPath.length() < 1 )
        {
            logger.debug( "No paths in cycle!" );
            return false;
        }

        if ( cachedRels != null && cycleMap != null )
        {
            for ( final long id : cyclicPath )
            {
                final IndexHits<Relationship> memberHits = cachedRels.get( RID, id );
                if ( !memberHits.hasNext() )
                {
                    ProjectRelationship<?> missingRel = cache.getRelationship( id );
                    if ( missingRel == null )
                    {
                        final Relationship missing = graph.getRelationshipById( id );
                        missingRel = toProjectRelationship( missing, cache );
                    }

                    if ( !cycleMap.containsKey( missingRel ) )
                    {
                        logger.debug( "Cycle relationship: {} not cached in view and not available in cycle mapping.", missingRel );
                        return false;
                    }
                }
            }
        }

        if ( cachedPathRels != null )
        {
            // 1. iterate relationships in the cycle path, since any of them could be a cycle entry point
            // 2. grab the start node id
            // 3. for all paths in the current view to that node
            //     A. retrieve the stored path and pathInfo
            //     B. verify acceptance through the cycle path
            //        1. extend the cached path by one for eath element in the cycle path
            //        2. verify and extend the pathInfo for each element
            //     C. verify acceptance of the injector using last pathInfo + full reconstructed path
            //     D. if all verifications pass, cache the cycle and move to the next cycle
            boolean found = false;
            entryPoint: for ( final long startRid : cyclicPath )
            {
                if ( startRid > -1 )
                {
                    cyclicPath.setEntryPoint( startRid );

                    final Relationship startR = graph.getRelationshipById( startRid );
                    final long startNid = startR.getStartNode()
                                                .getId();

                    final IndexHits<Relationship> pathHits = cachedPathRels.get( CACHED_PATH_TARGETS, startNid );
                    nextCachedPath: for ( final Relationship pathRel : pathHits )
                    {
                        Neo4jGraphPath viewPath = Conversions.getCachedPath( pathRel );
                        GraphPathInfo viewPathInfo = Conversions.getCachedPathInfo( pathRel, cache, this );
                        for ( final Long id : cyclicPath )
                        {
                            Relationship r = graph.getRelationshipById( id );

                            r = select( r, view, viewPathInfo, viewPath );

                            if ( r == null )
                            {
                                continue nextCachedPath;
                            }

                            final ProjectRelationship<?> rel = toProjectRelationship( r, cache );

                            viewPath = new Neo4jGraphPath( viewPath, id );
                            viewPathInfo = viewPathInfo.getChildPathInfo( rel );
                        }

                        found = true;
                        break entryPoint;
                    }
                }
            }

            if ( !found )
            {
                logger.debug( "Cycle is not wholly contained in view: {}", cyclicPath );
                return false;
            }
        }

        final IndexHits<Relationship> injectorHits = cyclePathRels.get( RID, injector.getId() );
        if ( !injectorHits.hasNext() )
        {
            final Relationship cyclePath = viewNode.createRelationshipTo( injector.getStartNode(), GraphRelType.CACHED_CYCLE_RELATIONSHIP );

            cyclePathRels.add( cyclePath, RID, injector.getId() );

            final CyclePath reoriented = cyclicPath.reorientToEntryPoint();
            logger.debug( "Caching cycle: {} in path relationship: {} (original was: {})", reoriented, cyclePath, cyclicPath );
            Conversions.storeCachedPath( reoriented, null, cyclePath );
            cyclePath.setProperty( RID, injector.getId() );
        }
        else
        {
            logger.debug( "Cycle is already contained in view cache: {}", cyclicPath );
        }

        return true;
    }

}
