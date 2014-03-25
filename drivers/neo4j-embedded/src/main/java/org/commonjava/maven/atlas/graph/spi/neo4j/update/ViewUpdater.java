package org.commonjava.maven.atlas.graph.spi.neo4j.update;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_CONTAINS_NODE;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_CONTAINS_REL;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_TARGETS;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.NID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.ViewIndexes;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AbstractTraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track.LuceneSeenTracker;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewUpdater
    extends AbstractTraverseVisitor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final GraphView view;

    private final Node viewNode;

    private final ConversionCache cache;

    private final GraphAdmin maint;

    //    private Map<Long, Neo4jGraphPath> toExtendPaths = new HashMap<Long, Neo4jGraphPath>();
    //
    //    private Map<Neo4jGraphPath, GraphPathInfo> toExtendPathInfoMap = new HashMap<Neo4jGraphPath, GraphPathInfo>();

    //    private final Set<Node> toExtendRoots = new HashSet<Node>();

    private final CycleCacheUpdater cycleUpdater;

    private final ViewIndexes indexes;

    private final long traverseId;

    public ViewUpdater( final GraphView view, final Node viewNode, final ViewIndexes indexes, final ConversionCache cache, final GraphAdmin maint )
    {
        super( new LuceneSeenTracker( view, viewNode, maint ) );
        this.traverseId = System.currentTimeMillis();
        this.view = view;
        this.viewNode = viewNode;
        this.indexes = indexes;
        this.cache = cache;
        this.maint = maint;
        this.cycleUpdater = new CycleCacheUpdater( view, viewNode, indexes, maint, cache );
    }

    public ViewUpdater( final GraphView view, final Node viewNode, final Map<Long, Neo4jGraphPath> toExtendPaths,
                        final Map<Neo4jGraphPath, GraphPathInfo> toExtendPathInfoMap, final ViewIndexes indexes, final ConversionCache cache,
                        final GraphAdmin maint )
    {
        super( new LuceneSeenTracker( view, viewNode, maint ) );
        this.traverseId = System.currentTimeMillis();
        this.view = view;
        this.viewNode = viewNode;
        this.indexes = indexes;
        this.cache = cache;
        this.maint = maint;
        //        this.toExtendPaths = toExtendPaths;
        //        this.toExtendPathInfoMap = toExtendPathInfoMap;
        this.cycleUpdater = new CycleCacheUpdater( view, viewNode, indexes, maint, cache );
    }

    public boolean processAddedRelationships( final Map<Long, ProjectRelationship<?>> createdRelationshipsMap )
    {
        boolean extend = false;
        for ( final Entry<Long, ProjectRelationship<?>> entry : createdRelationshipsMap.entrySet() )
        {
            final Long rid = entry.getKey();
            final Relationship add = maint.getRelationship( rid );
            //            final ProjectRelationship<?> rel = entry.getValue();

            //            if ( cycleMap != null && cycleMap.containsKey( rel ) )
            //            {
            //                continue;
            //            }

            indexes.getSelections()
                   .remove( add );

            final Node start = add.getStartNode();
            final IndexHits<Relationship> pathsRelHits = indexes.getCachedPaths()
                                                                .get( CACHED_PATH_TARGETS, start.getId() );

            final RelationshipIndex toExtendPaths = indexes.getToExtendPaths( traverseId );
            final Index<Node> toExtendNodes = indexes.getToExtendNodes( traverseId );

            if ( pathsRelHits.hasNext() )
            {
                final Relationship pathRel = pathsRelHits.next();

                final Map<Neo4jGraphPath, GraphPathInfo> pathInfoMap = Conversions.getCachedPathInfoMap( pathRel, view, cache );

                for ( final Entry<Neo4jGraphPath, GraphPathInfo> pathInfoEntry : pathInfoMap.entrySet() )
                {
                    Neo4jGraphPath path = pathInfoEntry.getKey();
                    GraphPathInfo pathInfo = pathInfoEntry.getValue();

                    final Relationship real = maint.select( add, view, pathInfo, path );

                    if ( real != null )
                    {
                        final ProjectRelationship<?> realRel = toProjectRelationship( real, cache );

                        pathInfo = pathInfo.getChildPathInfo( realRel );
                        path = new Neo4jGraphPath( path, real.getId() );

                        final Relationship pathCacheRel = cachePath( path, pathInfo );
                        if ( pathCacheRel != null )
                        {
                            toExtendPaths.add( pathCacheRel, RID, real.getId() );
                            toExtendNodes.add( real.getStartNode(), NID, real.getStartNode()
                                                                             .getId() );
                            extend = true;
                        }
                    }
                }
            }
        }

        return extend;
    }

    public Set<Node> getExtendRoots()
    {
        return Conversions.toSet( indexes.getToExtendNodes( traverseId )
                                         .query( NID, "*" ) );
    }

    @Override
    public void includingChild( final Relationship child, final Neo4jGraphPath childPath, final GraphPathInfo childPathInfo, final Path parentPath )
    {
        if ( parentPath.lastRelationship() != null )
        {
            logger.debug( "Parent is not empty, so no resume in effect; caching as-is: {} / {} / {} from: {}", child, childPath, childPathInfo,
                          parentPath );
            // we're not resuming a path here, so just cache it as-is and move on.
            cachePath( childPath, childPathInfo );
            return;
        }

        final RelationshipIndex toExtendPaths = indexes.getToExtendPaths( traverseId );
        final IndexHits<Relationship> hits = toExtendPaths.get( RID, child.getId() );
        if ( !hits.hasNext() )
        {
            logger.debug( "No resume available; caching as-is: {} / {} / {} from: {}", child, childPath, childPathInfo, parentPath );
            cachePath( childPath, childPathInfo );
        }
        else
        {
            final Relationship r = hits.next();
            final Set<Neo4jGraphPath> paths = Conversions.getCachedPaths( r );

            for ( final Neo4jGraphPath path : paths )
            {
                logger.debug( "Resuming: {} with child: {} / {} / {} from: {}", path, child, childPath, childPathInfo, parentPath );
                cachePath( path.append( childPath ), childPathInfo );
            }

            if ( paths.isEmpty() )
            {
                logger.debug( "No resume paths found, in spite of cached-paths relationship! (for: {})", child );
            }
        }
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setConversionCache( cache );
        collector.setTraverseId( traverseId );
    }

    private Relationship cachePath( final Neo4jGraphPath path, final GraphPathInfo pathInfo )
    {
        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( path, maint );
        if ( cyclePath != null )
        {
            logger.info( "CYCLE: {}", cyclePath );

            final Relationship injector = maint.getRelationship( path.getLastRelationshipId() );
            cycleUpdater.addCycle( cyclePath, injector );

            return null;
        }

        logger.debug( "Caching path: {} with pathInfo: {}", path, pathInfo );
        final RelationshipIndex cachedPaths = indexes.getCachedPaths();
        final IndexHits<Relationship> pathRelHits = cachedPaths.get( RID, path.getLastRelationshipId() );

        final long lastRelId = path.getLastRelationshipId();
        final Relationship lastRel = maint.getRelationship( lastRelId );
        final Node targetNode = lastRel.getEndNode();

        Relationship pathsRel;

        boolean existingRel = false;
        if ( pathRelHits.hasNext() )
        {
            pathsRel = pathRelHits.next();
            existingRel = true;

            if ( Conversions.hasCachedPath( path, pathsRel ) )
            {
                // already cached.
                return null;
            }
        }
        else
        {
            pathsRel = viewNode.createRelationshipTo( targetNode, GraphRelType.CACHED_PATH_RELATIONSHIP );
        }

        logger.debug( "Extending membership of view: {} with path: {}", viewNode, path );

        Conversions.storeCachedPath( path, pathInfo, pathsRel );

        if ( !existingRel )
        {
            cachedPaths.add( pathsRel, RID, path.getLastRelationshipId() );
        }

        logger.debug( "{}: {} += {}", view.getShortId(), CACHED_PATH_TARGETS, targetNode.getId() );
        cachedPaths.add( pathsRel, CACHED_PATH_TARGETS, targetNode.getId() );

        final RelationshipIndex cachedRels = indexes.getCachedRelationships();
        final Index<Node> cachedNodes = indexes.getCachedNodes();

        final Set<Long> nodes = new HashSet<Long>();
        for ( final Long relId : path )
        {
            final Relationship r = maint.getRelationship( relId );

            cachedPaths.add( pathsRel, CACHED_PATH_CONTAINS_REL, relId );
            cachedRels.add( r, RID, relId );

            final long startId = r.getStartNode()
                                  .getId();
            if ( nodes.add( startId ) )
            {
                cachedPaths.add( pathsRel, CACHED_PATH_CONTAINS_NODE, startId );
                cachedNodes.add( r.getStartNode(), NID, startId );
            }

            final long endId = r.getEndNode()
                                .getId();
            if ( nodes.add( endId ) )
            {
                cachedPaths.add( pathsRel, CACHED_PATH_CONTAINS_NODE, endId );
                cachedNodes.add( r.getEndNode(), NID, endId );
            }
        }

        return pathsRel;
    }

    @Override
    protected void traverseCompleting( final AtlasCollector<?> collector )
    {
        indexes.clearToExtendInfo( traverseId );
    }

    @Override
    public void cycleDetected( final CyclePath path, final Relationship injector )
    {
        cycleUpdater.cycleDetected( path, injector );
    }

}
