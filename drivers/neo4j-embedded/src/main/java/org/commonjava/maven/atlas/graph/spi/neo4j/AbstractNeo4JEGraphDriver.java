/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.spi.neo4j;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CONFIG_ID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GA;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.NID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RELATIONSHIP_ID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.SOURCE_URI;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.VIEW_ID;
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
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipComparator;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.MembershipWrappedTraversalEvaluator;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.PathCollectingVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.PathExistenceVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.SubPathsCollectingVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.ViewUpdater;
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

    //    private static final int ADD_BATCHSIZE = 50;

    private static final String ALL_RELATIONSHIPS = "all_relationships";

    private static final String BY_GAV_IDX = "by_gav";

    private static final String BY_GA_IDX = "by_ga";

    private static final String CONFIG_NODES_IDX = "config_nodes";

    private static final String VARIABLE_NODES_IDX = "variable_nodes";

    private static final String MISSING_NODES_IDX = "missing_nodes";

    private static final String METADATA_INDEX_PREFIX = "has_metadata_";

    private static final String MANAGED_GA = "managed-ga";

    private static final String MANAGED_KEY = "mkey";

    private static final String BASE_CONFIG_NODE = "_base";

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

    private final GraphAdminImpl adminAccess;

    protected AbstractNeo4JEGraphDriver( GraphWorkspaceConfiguration config, final GraphDatabaseService graph, final boolean useShutdownHook )
    {
        this.adminAccess = new GraphAdminImpl( this );

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
                    configNode = hits.next();
                    id = configNode.getId();
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
            return convertToRelationships( relationships, new ConversionCache() );
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

        final ConversionCache cache = new ConversionCache();
        if ( registerView( view ) )
        {
            final Node node = getNode( ref );

            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            final StringBuilder sb = new StringBuilder();
            for ( final Relationship r : relationships )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( ' ' );
                }

                sb.append( r.getId() );
            }

            final RelationshipIndex cachedRels = new ViewIndexes( graph.index(), view ).getCachedRelationships();
            final IndexHits<Relationship> hits = cachedRels.query( RID, sb.toString() );

            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();

            while ( hits.hasNext() )
            {
                final Relationship r = hits.next();
                final ProjectRelationship<?> rel = toProjectRelationship( r, cache );
                result.add( rel );
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
            return convertToRelationships( relationships, cache );
        }

        return null;
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        checkClosed();

        logger.debug( "Getting all-projects for: {}", view );
        final ConversionCache cache = new ConversionCache();
        if ( registerView( view ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), view ).getCachedNodes();

            final IndexHits<Node> nodeHits = cachedNodes.query( NID, "*" );
            final Set<ProjectVersionRef> nodes = new HashSet<ProjectVersionRef>();
            while ( nodeHits.hasNext() )
            {
                nodes.add( toProjectVersionRef( nodeHits.next(), cache ) );
            }

            return nodes;
        }

        // FIXME: What if this view has a filter or mutator?? Without a root, that would be very strange...
        return new HashSet<ProjectVersionRef>( convertToProjects( graph.index()
                                                                       .forNodes( BY_GAV_IDX )
                                                                       .query( GAV, "*" ), cache ) );
    }

    private void updateView( final GraphView view )
    {
        updateView( view, new ConversionCache() );
    }

    private void updateView( final GraphView view, final ConversionCache cache )
    {
        if ( view.getRoots() == null || view.getRoots()
                                            .isEmpty() )
        {
            logger.debug( "views without roots are never updated." );
            return;
        }

        final Node viewNode = getViewNode( view );

        logger.debug( "Checking whether {} ({} / {}) is in need of update.", view.getShortId(), viewNode, view );
        final ViewIndexes indexes = new ViewIndexes( graph.index(), view );

        if ( Conversions.isMembershipDetectionPending( viewNode ) )
        {
            logger.debug( "Traversing graph to update view membership: {} ({})", view.getShortId(), view );
            final Set<Node> roots = getRoots( view );
            if ( roots.isEmpty() )
            {
                logger.debug( "{}: No root nodes found.", view.getShortId() );
                return;
            }

            final ViewUpdater updater = new ViewUpdater( view, viewNode, indexes, cache, adminAccess );
            collectAtlasRelationships( view, updater, roots, false, Uniqueness.RELATIONSHIP_GLOBAL );

            logger.debug( "Traverse complete for update of view: {}", view.getShortId() );
        }
        else
        {
            logger.debug( "{}: no update pending.", view.getShortId() );
        }
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        checkClosed();

        final ConversionCache cache = new ConversionCache();
        if ( registerView( view ) )
        {
            final RelationshipIndex cachedRels = new ViewIndexes( graph.index(), view ).getCachedRelationships();

            final IndexHits<Relationship> relHits = cachedRels.query( RID, "*" );
            final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();
            while ( relHits.hasNext() )
            {
                rels.add( toProjectRelationship( relHits.next(), cache ) );
            }

            return rels;
        }

        final IndexHits<Relationship> hits = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS )
                                                  .query( RELATIONSHIP_ID, "*" );
        return convertToRelationships( hits, cache );
    }

    @Override
    public Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( final GraphView view, final Set<ProjectVersionRef> refs )
    {
        checkClosed();
        if ( !registerView( view ) )
        {
            throw new IllegalArgumentException( "You must specify at least one root GAV in order to retrieve path-related info." );
        }

        final Set<Node> endNodes = getNodes( refs );

        final ConversionCache cache = new ConversionCache();
        final PathCollectingVisitor visitor = new PathCollectingVisitor( endNodes, cache );
        collectAtlasRelationships( view, visitor, getRoots( view ), false, Uniqueness.RELATIONSHIP_GLOBAL );

        final Map<GraphPath<?>, GraphPathInfo> result = new HashMap<GraphPath<?>, GraphPathInfo>();
        for ( final Neo4jGraphPath path : visitor )
        {
            GraphPathInfo info = new GraphPathInfo( view );
            for ( final Long rid : path )
            {
                final Relationship r = graph.getRelationshipById( rid );
                info = info.getChildPathInfo( toProjectRelationship( r, cache ) );
            }

            result.put( path, info );
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

        return toProjectVersionRef( target, null );
    }

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final GraphView view, final ProjectVersionRef... refs )
    {
        checkClosed();
        if ( !registerView( view ) )
        {
            throw new IllegalArgumentException( "You must specify at least one root GAV in order to retrieve path-related info." );
        }

        final Set<Node> endNodes = getNodes( refs );

        final ConversionCache cache = new ConversionCache();
        final PathCollectingVisitor visitor = new PathCollectingVisitor( endNodes, cache );
        collectAtlasRelationships( view, visitor, getRoots( view ), false, Uniqueness.RELATIONSHIP_GLOBAL );

        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        for ( final Neo4jGraphPath path : visitor )
        {
            result.add( convertToRelationships( path, adminAccess, cache ) );
        }

        return result;
    }

    @Override
    public synchronized Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rels )
    {
        final Map<Long, ProjectRelationship<?>> createdRelationshipsMap = addRelationshipsInternal( rels );

        logger.info( "Updating all-projects caches with {} new entries", createdRelationshipsMap.size() );
        updateCaches( createdRelationshipsMap );

        // FIXME: We're delaying cycle detection, so there will NEVER be rejected relationships...
        final Set<ProjectRelationship<?>> skipped = Collections.emptySet();
        logger.debug( "Cycle injection detected for: {}", skipped );
        logger.info( "Returning {} rejected relationships.", skipped.size() );

        //        printGraphStats();

        return skipped;
    }

    private Map<Long, ProjectRelationship<?>> addRelationshipsInternal( final ProjectRelationship<?>... rels )
    {
        checkClosed();

        final ConversionCache cache = new ConversionCache();
        final Map<Long, ProjectRelationship<?>> createdRelationshipsMap = new HashMap<Long, ProjectRelationship<?>>();

        final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( Arrays.asList( rels ) );
        Collections.sort( sorted, RelationshipComparator.INSTANCE );

        final Transaction tx = graph.beginTx();
        try
        {
            //            int txBatchCount = 0;
            nextRel: for ( final ProjectRelationship<?> rel : sorted )
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
                            logger.debug( "Node: {} created for: {}", node, ref );
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

                    logger.debug( "Removing missing/incomplete flag from: {} ({})", from, declaring );
                    graph.index()
                         .forNodes( MISSING_NODES_IDX )
                         .remove( from );

                    markConnected( from, true );

                    if ( from.getId() != nodes[1].getId() )
                    {
                        final Node to = nodes[1];

                        logger.debug( "Creating graph relationship for: {} between node: {} and node: {}", rel, from, to );

                        final GraphRelType grt = GraphRelType.map( rel.getType(), rel.isManaged() );

                        relationship = from.createRelationshipTo( to, grt );

                        // now, we set an index on the relationship of where it is in the range of ALL atlas relationships 
                        // for this node. ProjectRelationship<?>.getIndex() only gives the index for that TYPE, so we can't
                        // use it. The next value will be stored on the from node for the next go.
                        int nodeRelIdx = Conversions.getIntegerProperty( Conversions.ATLAS_RELATIONSHIP_COUNT, from, 0 );

                        relationship.setProperty( Conversions.ATLAS_RELATIONSHIP_INDEX, nodeRelIdx );
                        from.setProperty( Conversions.ATLAS_RELATIONSHIP_COUNT, ++nodeRelIdx );

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

                        logger.debug( "+= {} ({})", relationship, rel );
                    }
                    else
                    {
                        logger.info( "Self-referential relationship: {}. Skipping", rel );
                        continue;
                    }

                    if ( !( rel instanceof ParentRelationship ) || !( (ParentRelationship) rel ).isTerminus() )
                    {
                        createdRelationshipsMap.put( relationship.getId(), rel );
                    }
                }
                else
                {
                    relationship = relHits.next();
                    logger.debug( "== {} ({})", relationship, toProjectRelationship( relationship, cache ) );

                    addToURISetProperty( rel.getSources(), SOURCE_URI, relationship );
                }

                // We don't really need transaction here, I don't think...
                // but we're forced to use one to update the graph.
                // So, to find a balance between memory consumed by a transaction 
                // and speed (creating/committing txns takes time), we'll batch them.
                //                txBatchCount++;
                //                if ( txBatchCount >= ADD_BATCHSIZE )
                //                {
                //                    logger.info( "Storing batch of {} relationships.", txBatchCount + 1 );
                //                    tx.success();
                //                    tx = graph.beginTx();
                //                    txBatchCount = 0;
                //                }
            }

            //            logger.info( "Storing final batch of {} relationships.", txBatchCount + 1 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return createdRelationshipsMap;
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

        logger.debug( "Checking for existence of path from: {} to: {} in global database", fromNode, toNode );
        final PathExistenceVisitor collector = new PathExistenceVisitor( toNode );
        collectAtlasRelationships( view, collector, Collections.singleton( fromNode ), false, Uniqueness.RELATIONSHIP_GLOBAL );

        return collector.isFound();
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

    private Relationship select( final Relationship old, final GraphView view, final Node viewNode, final GraphPathInfo pathInfo,
                                 final Neo4jGraphPath path )
    {
        final ViewIndexes indexes = new ViewIndexes( graph.index(), view );

        final long targetRid = Conversions.getDeselectionTarget( old.getId(), viewNode );
        if ( targetRid > -1 )
        {
            return graph.getRelationshipById( targetRid );
        }

        final ProjectRelationship<?> oldRel = toProjectRelationship( old, null );
        if ( oldRel == null )
        {
            return null;
        }

        logger.debug( "Selecting mutated relationship for: {} with pathInfo: {}", oldRel, pathInfo );
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

            logger.info( "Creating ad-hoc db relationship for selection: {} (replacing: {})", selected, oldRel );

            @SuppressWarnings( "unused" )
            final Map<Long, ProjectRelationship<?>> added = addRelationshipsInternal( selected );

            final Transaction tx = graph.beginTx();
            try
            {
                Relationship result = null;
                hits = relIdx.get( RELATIONSHIP_ID, selId );
                if ( hits.hasNext() )
                {
                    result = hits.next();

                    logger.debug( "Adding relationship {} to selections index", result );
                    Conversions.setSelection( old.getId(), result.getId(), viewNode );

                    // Does this imply that a whole subgraph from oldRel needs to be removed from the cache??
                    // No, because that would only happen if a new selection were added to the view, which would trigger a registerViewSelection() call...
                    logger.debug( "Adding node {} to membership cache for {}", result.getEndNode()
                                                                                     .getId(), view.getShortId() );
                    indexes.getCachedNodes()
                           .add( result.getEndNode(), NID, result.getEndNode()
                                                                 .getId() );
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

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            //            logger.debug( "PASS: {}", i );

            // NOTE: Changing this means some cases of morphing filters/mutators may NOT report correct results.
            //            TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_PATH )
            TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL )
                                                        .sort( PathComparator.INSTANCE );

            final GraphRelType[] relTypes = getGraphRelTypes( view.getFilter() );
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

            final ConversionCache cache = new ConversionCache();

            final Node viewNode = getViewNode( view );

            @SuppressWarnings( { "rawtypes", "unchecked" } )
            final MembershipWrappedTraversalEvaluator checker =
                new MembershipWrappedTraversalEvaluator( Collections.singleton( rootNode.getId() ), traversal, view, viewNode, adminAccess, i,
                                                         relTypes );

            checker.setConversionCache( cache );

            description = description.expand( checker )
                                     .evaluator( checker );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                if ( path.lastRelationship() == null )
                {
                    continue;
                }

                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships(), cache );
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

    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        checkClosed();

        final IndexHits<Node> missing = graph.index()
                                             .forNodes( MISSING_NODES_IDX )
                                             .get( GAV, ref.asProjectVersionRef()
                                                           .toString() );
        if ( missing.size() > 0 )
        {
            return false;
        }

        final Node node = getNode( ref );
        if ( node == null )
        {
            return false;
        }

        if ( registerView( view ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), view ).getCachedNodes();

            final IndexHits<Node> nodeHits = cachedNodes.get( NID, node.getId() );
            return nodeHits.hasNext();
        }
        else
        {
            return getNode( ref ) != null;
        }
    }

    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        checkClosed();

        final Relationship relationship = getRelationship( rel );
        if ( relationship == null )
        {
            return false;
        }

        if ( registerView( view ) )
        {
            return new ViewIndexes( graph.index(), view ).getCachedRelationships()
                                                         .get( RID, relationship.getId() )
                                                         .hasNext();
        }
        else
        {
            return true;
        }
    }

    private Set<Node> getNodes( final Set<ProjectVersionRef> refs )
    {
        final Set<Node> nodes = new HashSet<Node>( refs.size() );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node node = getNode( ref );
            if ( node != null )
            {
                nodes.add( node );
            }
        }

        return nodes;
    }

    private Set<Node> getNodes( final ProjectVersionRef... refs )
    {
        final Set<Node> nodes = new HashSet<Node>( refs.length );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node node = getNode( ref );
            if ( node != null )
            {
                nodes.add( node );
            }
        }

        return nodes;
    }

    protected Node getNode( final ProjectVersionRef ref )
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

        return node;
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
        logger.debug( "Getting missing projects for: {}", view.getShortId() );
        return getIndexedProjects( view, MISSING_NODES_IDX );
    }

    private Set<ProjectVersionRef> getIndexedProjects( final GraphView view, final String indexName )
    {
        checkClosed();

        final IndexHits<Node> hits = graph.index()
                                          .forNodes( indexName )
                                          .query( GAV, "*" );

        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();

        final ConversionCache cache = new ConversionCache();
        if ( registerView( view ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), view ).getCachedNodes();

            for ( final Node node : hits )
            {
                logger.debug( "Checking for membership: {} ({})", node, node.getProperty( GAV ) );
                final IndexHits<Node> cacheHits = cachedNodes.get( NID, node.getId() );
                if ( cacheHits.hasNext() )
                {
                    logger.debug( "Including: {}", node );
                    result.add( toProjectVersionRef( node, cache ) );
                }
            }
        }
        else
        {
            for ( final Node node : hits )
            {
                logger.debug( "Including: {}", node );
                result.add( toProjectVersionRef( node, cache ) );
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
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), view ).getCachedNodes();

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
        return getRoots( view, true );
    }

    private Set<Node> getRoots( final GraphView view, final boolean defaultToAll )
    {
        final Set<ProjectVersionRef> rootRefs = view.getRoots();
        if ( ( rootRefs == null || rootRefs.isEmpty() ) && defaultToAll )
        {
            return Collections.emptySet();
            //            final Set<Node> connectedNodes = Conversions.toSet( graph.index()
            //                                                                     .forNodes( BY_GAV_IDX )
            //                                                                     .query( GAV, "*" ) );
            //            connectedNodes.removeAll( Conversions.toSet( graph.index()
            //                                                              .forNodes( MISSING_NODES_IDX )
            //                                                              .query( GAV, "*" ) ) );
            //            connectedNodes.removeAll( Conversions.toSet( graph.index()
            //                                                              .forNodes( VARIABLE_NODES_IDX )
            //                                                              .query( GAV, "*" ) ) );
            //
            //            return connectedNodes;
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

    private void collectAtlasRelationships( final GraphView view, final TraverseVisitor visitor, final Set<Node> start, final boolean sorted,
                                            final Uniqueness uniqueness )
    {
        if ( start == null || start.isEmpty() )
        {
            throw new UnsupportedOperationException( "Cannot collect atlas nodes/relationships via traversal without at least one 'from' node!" );
        }

        //        logger.info( "Traversing for aggregation using: {} from roots: {}", checker.getClass()
        //                                                                                   .getName(), from );

        TraversalDescription description = Traversal.traversal( uniqueness );
        if ( sorted )
        {
            description = description.sort( PathComparator.INSTANCE );
        }

        final GraphRelType[] relTypes = getGraphRelTypes( view.getFilter() );
        for ( final GraphRelType grt : relTypes )
        {
            description.relationships( grt, Direction.OUTGOING );
        }

        description = description.breadthFirst();

        final Node viewNode = getViewNode( view );

        final AtlasCollector<Object> checker = new AtlasCollector<Object>( visitor, start, view, viewNode, adminAccess, relTypes );
        description = description.expand( checker )
                                 .evaluator( checker );

        try
        {
            final Traverser traverser = description.traverse( start.toArray( new Node[] {} ) );
            for ( @SuppressWarnings( "unused" )
            final Path path : traverser )
            {
                //            logger.info( "Aggregating path: {}", path );
                // Don't need this, but we need to iterate the traverser.
            }
        }
        finally
        {
            visitor.traverseComplete( checker );
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
        logger.debug( "Getting variable projects for: {}", view.getShortId() );
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

        final ViewIndexes indexes = new ViewIndexes( graph.index(), view );

        if ( !registerView( view ) )
        {
            logger.warn( "Skipping cycle detection on {}. View doesn't declare a root GAV, and this is prohibitively expensive! (view info: {})",
                         view.getShortId(), view );
            return null;
        }

        final ConversionCache cache = new ConversionCache();

        final Transaction tx = graph.beginTx();
        Node viewNode;
        try
        {
            viewNode = getViewNode( view );

            if ( Conversions.isCycleDetectionPending( viewNode ) )
            {
                logger.debug( "Cycle-detection is pending for: {}", view.getShortId() );

                Set<Node> nodes;
                //                if ( global )
                //                {
                //                    // FIXME: This seems to be VERY expensive
                //                    nodes = Conversions.toSet( graph.index()
                //                                                    .forNodes( BY_GAV_IDX )
                //                                                    .query( GAV, "*" ) );
                //                }
                //                else
                //                {
                nodes = Conversions.toSet( indexes.getCachedNodes()
                                                  .query( NID, "*" ) );
                //                }

                logger.info( "Traversing graph to find cycles for view {}", view.getShortId() );
                final CycleCacheUpdater cycleUpdater = new CycleCacheUpdater( view, viewNode, adminAccess, cache );
                // NOTE: Changing this means some cases of morphing filters/mutators may NOT report correct results.
                //                collectAtlasRelationships( view, cycleUpdater, nodes, false, global ? Uniqueness.RELATIONSHIP_GLOBAL : Uniqueness.RELATIONSHIP_PATH );
                collectAtlasRelationships( view, cycleUpdater, nodes, false, Uniqueness.RELATIONSHIP_GLOBAL );

                final int cycleCount = cycleUpdater.getCycleCount();
                logger.info( "Registered {} cycles in view {}'s cycle cache.", cycleCount, view.getShortId() );

                return cycleUpdater.getCycles();
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        final Set<CyclePath> cyclePaths = Conversions.getCachedCyclePaths( viewNode );
        logger.debug( "Retrieved the following cached cycle paths:\n  {}", new JoinString( "\n  ", cyclePaths ) );

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
        for ( final CyclePath cyclicPath : cyclePaths )
        {
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

            logger.debug( "[cache] CYCLES += {}", cycle );
            cycles.add( new EProjectCycle( cycle ) );
        }

        return cycles;
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
    public void addMetadata( final ProjectVersionRef ref, final String key, final String value )
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
    public void setMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
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

    private void checkExecutionEngine()
    {
        if ( queryEngine == null )
        {
            queryEngine = new ExecutionEngine( graph );
        }
    }

    @Override
    public void reindex()
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final IndexHits<Node> nodes = graph.index()
                                               .forNodes( BY_GAV_IDX )
                                               .query( GAV, "*" );
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
        checkClosed();

        final IndexHits<Node> nodes = graph.index()
                                           .forNodes( METADATA_INDEX_PREFIX + key )
                                           .query( GAV, "*" );

        final ConversionCache cache = new ConversionCache();
        if ( registerView( view ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), view ).getCachedNodes();

            final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
            for ( final Node node : nodes )
            {
                if ( cachedNodes.get( NID, node.getId() )
                                .hasNext() )
                {
                    result.add( toProjectVersionRef( node, cache ) );
                }
            }

            return result;
        }
        else
        {
            return new HashSet<ProjectVersionRef>( convertToProjects( nodes, cache ) );
        }
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !containsProject( null, ref ) )
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

            final ConversionCache cache = new ConversionCache();
            for ( final Relationship r : relationships )
            {
                if ( TraversalUtils.acceptedInView( r, view, cache ) )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r, cache );
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

        final ConversionCache cache = new ConversionCache();
        if ( relationships != null )
        {
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            for ( final Relationship r : relationships )
            {
                logger.debug( "Examining relationship: {}", r );
                if ( TraversalUtils.acceptedInView( r, view, cache ) )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r, cache );
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
    public Set<ProjectVersionRef> getProjectsMatching( final GraphView eProjectNetView, final ProjectRef projectRef )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( BY_GA_IDX )
                                          .query( GA, projectRef.asProjectRef()
                                                                .toString() );
        return new HashSet<ProjectVersionRef>( convertToProjects( hits, new ConversionCache() ) );
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

        final ConversionCache cache = new ConversionCache();
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
                                                                       .getEndNode(), cache );

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
    public List<ProjectVersionRef> getPathRefs( final GraphView view, final GraphPath<?> path )
    {
        if ( path != null && !( path instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "Cannot get refs for: " + path + ". This is not a Neo4jGraphPathKey instance!" );
        }

        final Neo4jGraphPath gp = (Neo4jGraphPath) path;
        final List<ProjectRelationship<?>> rels = convertToRelationships( gp, adminAccess, new ConversionCache() );
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>( rels.size() + 2 );
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( refs.isEmpty() )
            {
                refs.add( rel.getDeclaring() );
            }

            refs.add( rel.getTarget()
                         .asProjectVersionRef() );
        }

        return refs;
    }

    @Override
    public GraphPath<?> createPath( final ProjectRelationship<?>... rels )
    {
        final long[] relIds = new long[rels.length];
        for ( int i = 0; i < rels.length; i++ )
        {
            final Relationship r = getRelationship( rels[i] );
            if ( r == null )
            {
                return null;
            }

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
            final Transaction tx = graph.beginTx();
            try
            {
                logger.debug( "Creating new node to account for missing project referenced in path: {}", r );

                addRelationshipsInternal( rel );

                // FIXME: Restore cycle detection somehow...
                //                    if ( rejected != null && !rejected.isEmpty() )
                //                    {
                //                        tx.failure();
                //                        throw new IllegalArgumentException( "Cannot create missing relationship for: " + rel + ". It creates a relationship cycle." );
                //                    }

                r = getRelationship( rel );

                tx.success();
                if ( r == null )
                {
                    return null;
                }
            }
            finally
            {
                tx.finish();
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
        if ( view == null )
        {
            return false;
        }

        if ( view.getRoots() == null || view.getRoots()
                                            .isEmpty() )
        {
            logger.info( "Cannot track membership in view! It has no root GAVs.\nView: {} (short id: {})", view.getLongId(), view.getShortId() );
            return false;
        }

        updateView( view );

        return true;
    }

    private Node getViewNode( final GraphView view )
    {
        if ( view.equals( globalView ) )
        {
            return configNode;
        }

        final Index<Node> confIdx = graph.index()
                                         .forNodes( CONFIG_NODES_IDX );

        final IndexHits<Node> hits = confIdx.get( VIEW_ID, view.getShortId() );
        if ( hits.hasNext() )
        {
            logger.debug( "View already registered: {} (short id: {})", view.getLongId(), view.getShortId() );
            return hits.next();
        }
        else
        {
            logger.debug( "Registering new view: {} (short id: {})", view.getLongId(), view.getShortId() );

            final Transaction tx = graph.beginTx();
            try
            {
                final Node viewNode;
                viewNode = graph.createNode();
                Conversions.storeView( view, viewNode );

                confIdx.add( viewNode, VIEW_ID, view.getShortId() );

                logger.debug( "Setting cycle-detection PENDING for new viewNode: {} of: {}", viewNode, view.getShortId() );
                Conversions.setCycleDetectionPending( viewNode, true );
                Conversions.setMembershipDetectionPending( viewNode, true );

                final ViewIndexes indexes = new ViewIndexes( graph.index(), view );
                final Index<Node> cachedNodes = indexes.getCachedNodes();

                for ( final ProjectVersionRef rootRef : view.getRoots() )
                {
                    Node rootNode = getNode( rootRef );
                    if ( rootNode == null )
                    {
                        logger.info( "Creating node for root: {}", rootRef );
                        rootNode = newProjectNode( rootRef );
                    }

                    cachedNodes.add( rootNode, NID, rootNode.getId() );
                }

                tx.success();

                return viewNode;
            }
            finally
            {
                tx.finish();
            }
        }
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

        final Set<Long> viaNodes = new HashSet<Long>();
        for ( final Node node : nodeHits )
        {
            viaNodes.add( node.getId() );
        }

        logger.debug( "Searching for sub-paths to de-select (via): {}", viaNodes );

        final Set<Node> toUncacheNode = new HashSet<Node>();
        final Set<Relationship> toUncache = new HashSet<Relationship>();
        final Set<Relationship> toUnselect = new HashSet<Relationship>();

        final Transaction tx = graph.beginTx();
        try
        {
            final SubPathsCollectingVisitor visitor = new SubPathsCollectingVisitor( viaNodes, adminAccess );
            collectAtlasRelationships( view, visitor, getRoots( view ), false, Uniqueness.RELATIONSHIP_GLOBAL );

            for ( final Neo4jGraphPath path : visitor )
            {
                boolean uncache = false;
                for ( final Long id : path )
                {
                    final Relationship r = graph.getRelationshipById( id );

                    // first relationship in the sub-path.
                    if ( !uncache )
                    {
                        logger.debug( "Uncaching subgraph: {}", r.getEndNode() );
                        logger.debug( "Uncaching: {}", r );
                        toUncache.add( r );
                        uncache = true;
                        continue;
                    }
                    else
                    {
                        logger.debug( "Uncaching: {}", r );
                        toUncacheNode.add( r.getStartNode() );
                        toUncacheNode.add( r.getEndNode() );
                        toUncache.add( r );
                        continue;
                    }
                }
            }

            final ViewIndexes indexes = new ViewIndexes( graph.index(), view );
            final Index<Node> nodes = indexes.getCachedNodes();
            for ( final Node uncache : toUncacheNode )
            {
                logger.debug( "Uncache: {}", uncache );
                nodes.remove( uncache );
            }

            final RelationshipIndex rels = indexes.getCachedRelationships();
            for ( final Relationship uncache : toUncache )
            {
                logger.debug( "Uncache: {}", uncache );
                rels.remove( uncache );
            }

            final Node viewNode = getViewNode( view );
            for ( final Relationship unsel : toUnselect )
            {
                Conversions.removeSelectionByTarget( unsel.getId(), viewNode );
            }

            Conversions.setMembershipDetectionPending( viewNode, true );
            Conversions.setCycleDetectionPending( viewNode, true );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private void updateCaches( final Map<Long, ProjectRelationship<?>> newRelationships )
    {
        if ( newRelationships.isEmpty() )
        {
            return;
        }

        final Index<Node> confIdx = graph.index()
                                         .forNodes( CONFIG_NODES_IDX );

        final IndexHits<Node> hits = confIdx.query( VIEW_ID, "*" );

        final ConversionCache cache = new ConversionCache();

        Transaction tx = graph.beginTx();
        try
        {
            logger.debug( "Setting global cycle-detection as PENDING" );
            Conversions.setCycleDetectionPending( configNode, true );

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        for ( final Node viewNode : hits )
        {
            final GraphView view = Conversions.retrieveView( viewNode, cache, adminAccess );
            logger.debug( "Updating view: {} ({})", view.getShortId(), viewNode );

            if ( view == null || view.getShortId()
                                     .equals( globalView.getShortId() ) || view.getRoots() == null || view.getRoots()
                                                                                                          .isEmpty() )
            {
                logger.debug( "nevermind; it's the global view." );
                continue;
            }

            if ( getRoots( view, false ).isEmpty() )
            {
                tx = graph.beginTx();
                try
                {
                    Conversions.setCycleDetectionPending( viewNode, true );
                    //                    Conversions.setMembershipDetectionPending( viewNode, true );
                    tx.success();
                    continue;
                }
                finally
                {
                    tx.finish();
                }
            }

            final ViewIndexes vi = new ViewIndexes( graph.index(), view );
            final ViewUpdater vu = new ViewUpdater( view, viewNode, vi, cache, adminAccess );
            vu.cacheRoots( getRoots( view, false ) );
            if ( vu.processAddedRelationships( newRelationships ) )
            {
                logger.debug( "{} ({}) marked for update.", view.getShortId(), viewNode );
            }
            else
            {
                logger.debug( "{} ({}) NOT marked for update.", view.getShortId(), viewNode );
            }
        }
    }

    private static class GraphAdminImpl
        implements GraphAdmin
    {

        private final AbstractNeo4JEGraphDriver driver;

        GraphAdminImpl( final AbstractNeo4JEGraphDriver driver )
        {
            this.driver = driver;
        }

        @Override
        public AbstractNeo4JEGraphDriver getDriver()
        {
            return driver;
        }

        @Override
        public Relationship getRelationship( final long rid )
        {
            return driver.graph.getRelationshipById( rid );
        }

        @Override
        public Relationship select( final Relationship r, final GraphView view, final Node viewNode, final GraphPathInfo viewPathInfo,
                                    final Neo4jGraphPath viewPath )
        {
            return driver.select( r, view, viewNode, viewPathInfo, viewPath );
        }

        @Override
        public RelationshipIndex getRelationshipIndex( final String name )
        {
            return driver.graph.index()
                               .forRelationships( name );
        }

        @Override
        public Index<Node> getNodeIndex( final String name )
        {
            return driver.graph.index()
                               .forNodes( name );
        }

        @Override
        public Transaction beginTransaction()
        {
            return driver.graph.beginTx();
        }

        @Override
        public boolean isSelection( final Relationship r, final Node viewNode )
        {
            return Conversions.getDeselectionTarget( r.getId(), viewNode ) > -1;
        }

    }

}
