package org.commonjava.maven.atlas.graph.spi.neo4j;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_CONTAINS_NODE;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_CONTAINS_REL;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_RELATIONSHIP;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_TARGETS;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.NID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AbstractTraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
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

    private final RelationshipIndex cachedPathRels;

    private final Index<Node> cachedNodes;

    private final RelationshipIndex cachedRels;

    private final RelationshipIndex cyclePathRels;

    private final ConversionCache cache;

    private final GraphMaintenance maint;

    private final CycleCacheUpdater cycleUpdater;

    private final Set<CyclePath> seenCycles = new HashSet<CyclePath>();

    private Map<Long, Neo4jGraphPath> toExtendPaths = new HashMap<Long, Neo4jGraphPath>();

    private Map<Neo4jGraphPath, GraphPathInfo> toExtendPathInfoMap = new HashMap<Neo4jGraphPath, GraphPathInfo>();

    private final Set<Node> toExtendRoots = new HashSet<Node>();

    private int cycleCount = 0;

    public ViewUpdater( final GraphView view, final Node viewNode, final RelationshipIndex cachedPathRels, final RelationshipIndex cachedRels,
                        final Index<Node> cachedNodes, final RelationshipIndex cyclePathRels, final ConversionCache cache,
                        final GraphMaintenance maint, final CycleCacheUpdater cycleUpdater )
    {
        this.view = view;
        this.viewNode = viewNode;
        this.cachedPathRels = cachedPathRels;
        this.cachedRels = cachedRels;
        this.cachedNodes = cachedNodes;
        this.cyclePathRels = cyclePathRels;
        this.cache = cache;
        this.maint = maint;
        this.cycleUpdater = cycleUpdater;
    }

    public ViewUpdater( final GraphView view, final Node viewNode, final Map<Long, Neo4jGraphPath> toExtendPaths,
                        final Map<Neo4jGraphPath, GraphPathInfo> toExtendPathInfoMap, final RelationshipIndex cachedPathRels,
                        final RelationshipIndex cachedRels, final Index<Node> cachedNodes, final RelationshipIndex cyclePathRels,
                        final ConversionCache cache, final GraphMaintenance maint, final CycleCacheUpdater cycleUpdater )
    {
        this.view = view;
        this.viewNode = viewNode;
        this.cachedPathRels = cachedPathRels;
        this.cachedRels = cachedRels;
        this.cachedNodes = cachedNodes;
        this.cyclePathRels = cyclePathRels;
        this.cache = cache;
        this.maint = maint;
        this.cycleUpdater = cycleUpdater;
        this.toExtendPaths = toExtendPaths;
        this.toExtendPathInfoMap = toExtendPathInfoMap;
    }

    public void processAddedRelationships( final Map<Long, ProjectRelationship<?>> createdRelationshipsMap )
    {
        for ( final Entry<Long, ProjectRelationship<?>> entry : createdRelationshipsMap.entrySet() )
        {
            final Long rid = entry.getKey();
            final Relationship add = maint.getRelationship( rid );
            //            final ProjectRelationship<?> rel = entry.getValue();

            //            if ( cycleMap != null && cycleMap.containsKey( rel ) )
            //            {
            //                continue;
            //            }

            final Node start = add.getStartNode();
            final IndexHits<Relationship> pathsRelHits = cachedPathRels.get( CACHED_PATH_TARGETS, start.getId() );
            for ( final Relationship pathRel : pathsRelHits )
            {
                Neo4jGraphPath path = Conversions.getCachedPath( pathRel );
                GraphPathInfo pathInfo = Conversions.getCachedPathInfo( pathRel, cache, maint );

                final Relationship real = maint.select( add, view, pathInfo, path );

                if ( real != null )
                {
                    final ProjectRelationship<?> realRel = toProjectRelationship( real, cache );

                    pathInfo = pathInfo.getChildPathInfo( realRel );
                    path = new Neo4jGraphPath( path, real.getId() );

                    cachePath( path, pathInfo );

                    toExtendPathInfoMap.put( new Neo4jGraphPath( real.getId() ), pathInfo );
                    toExtendPaths.put( real.getId(), path );
                    toExtendRoots.add( real.getEndNode() );
                }
            }
        }

        //        extendCachedPaths( toExtend, toExtendRoots, view, viewNode, cachedPathRels, cachedRels, cachedNodes );
    }

    @Override
    public void cycleDetected( final Path path )
    {
        final CyclePath cpath = new CyclePath( path );
        final Relationship last = path.lastRelationship();

        if ( cycleUpdater.cacheCycle( cpath, last, cyclePathRels, cachedPathRels, cachedRels, view, viewNode, seenCycles ) )
        {
            cycleCount++;
        }
    }

    public Collection<? extends Node> getExtendRoots()
    {
        return toExtendRoots;
    }

    public Map<Neo4jGraphPath, GraphPathInfo> getExtendPathInfoMap()
    {
        return toExtendPathInfoMap;
    }

    @Override
    public void includingChild( final Relationship child, final Neo4jGraphPath childPath, final GraphPathInfo childPathInfo, final Path parentPath )
    {
        if ( toExtendPaths.isEmpty() )
        {
            cachePath( childPath, childPathInfo );
        }
        else
        {
            final Neo4jGraphPath path = toExtendPaths.get( childPath.getFirstRelationshipId() );
            if ( path != null )
            {
                cachePath( path.append( childPath ), childPathInfo );
            }
        }
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setAvoidCycles( false );
        collector.setConversionCache( cache );
        collector.setPathInfoMap( getExtendPathInfoMap() );
    }

    private void cachePath( final Neo4jGraphPath path, final GraphPathInfo pathInfo )
    {
        logger.debug( "Caching path: {} with pathInfo: {}", path, pathInfo );
        final String key = path.getKey();
        final IndexHits<Relationship> pathRelHits = cachedPathRels.get( CACHED_PATH_RELATIONSHIP, key );

        if ( pathRelHits.hasNext() )
        {
            // already cached.
            return;
        }

        logger.info( "Extending membership of view: {} with path: {}", viewNode, path );

        final long lastRelId = path.getLastRelationshipId();
        final Relationship lastRel = maint.getRelationship( lastRelId );
        final Node targetNode = lastRel.getEndNode();

        final Relationship pathsRel = viewNode.createRelationshipTo( targetNode, GraphRelType.CACHED_PATH_RELATIONSHIP );
        Conversions.storeCachedPath( path, pathInfo, pathsRel );

        cachedPathRels.add( pathsRel, CACHED_PATH_RELATIONSHIP, key );

        logger.debug( "{}: {} += {}", view.getShortId(), CACHED_PATH_TARGETS, targetNode.getId() );
        cachedPathRels.add( pathsRel, CACHED_PATH_TARGETS, targetNode.getId() );

        final Set<Long> nodes = new HashSet<Long>();
        for ( final Long relId : path )
        {
            final Relationship r = maint.getRelationship( relId );

            cachedPathRels.add( pathsRel, CACHED_PATH_CONTAINS_REL, relId );
            cachedRels.add( r, RID, relId );

            final long startId = r.getStartNode()
                                  .getId();
            if ( nodes.add( startId ) )
            {
                cachedPathRels.add( pathsRel, CACHED_PATH_CONTAINS_NODE, startId );
                cachedNodes.add( r.getStartNode(), NID, startId );
            }

            final long endId = r.getEndNode()
                                .getId();
            if ( nodes.add( endId ) )
            {
                cachedPathRels.add( pathsRel, CACHED_PATH_CONTAINS_NODE, endId );
                cachedNodes.add( r.getEndNode(), NID, endId );
            }
        }
    }

    public int getCycleCount()
    {
        return cycleCount;
    }

}
