package org.commonjava.maven.atlas.graph.spi.neo4j.update;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_TARGETS;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
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
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CycleCacheUpdater
    extends AbstractTraverseVisitor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ConversionCache cache;

    private final GraphAdmin maint;

    private final Set<CyclePath> seenCycles = new HashSet<CyclePath>();

    private final GraphView view;

    private final Node viewNode;

    final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final ViewIndexes indexes;

    public CycleCacheUpdater( final GraphView view, final Node viewNode, final ViewIndexes indexes, final GraphAdmin maint,
                              final ConversionCache cache )
    {
        super( new LuceneSeenTracker( view, viewNode, maint ) );
        this.indexes = indexes;
        this.view = view;
        this.viewNode = viewNode;
        this.maint = maint;
        this.cache = cache;
    }

    @Override
    public void cycleDetected( final CyclePath cyclicPath, final Relationship injector )
    {
        if ( !seenCycles.add( cyclicPath ) )
        {
            logger.debug( "Already seen cycle path: {}", cyclicPath );
            return;
        }

        if ( cyclicPath.length() < 1 )
        {
            logger.debug( "No paths in cycle!" );
            return;
        }

        //        if ( cachedRels != null && cycleMap != null )
        //        {
        //            for ( final long id : cyclicPath )
        //            {
        //                final IndexHits<Relationship> memberHits = cachedRels.get( RID, id );
        //                if ( !memberHits.hasNext() )
        //                {
        //                    ProjectRelationship<?> missingRel = cache.getRelationship( id );
        //                    if ( missingRel == null )
        //                    {
        //                        final Relationship missing = graph.getRelationshipById( id );
        //                        missingRel = toProjectRelationship( missing, cache );
        //                    }
        //
        //                    if ( !cycleMap.containsKey( missingRel ) )
        //                    {
        //                        logger.debug( "Cycle relationship: {} not cached in view and not available in cycle mapping.", missingRel );
        //                        return false;
        //                    }
        //                }
        //            }
        //        }

        final RelationshipIndex cachedPaths = indexes.getCachedPaths();

        if ( cachedPaths != null )
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
                if ( startRid == injector.getId() )
                {
                    continue;
                }

                if ( startRid > -1 )
                {
                    cyclicPath.setEntryPoint( startRid );

                    final Relationship startR = maint.getRelationship( startRid );
                    final long startNid = startR.getStartNode()
                                                .getId();

                    final IndexHits<Relationship> pathHits = cachedPaths.get( CACHED_PATH_TARGETS, startNid );
                    if ( pathHits.hasNext() )
                    {
                        final Relationship pathRel = pathHits.next();
                        final Map<Neo4jGraphPath, GraphPathInfo> map = Conversions.getCachedPathInfoMap( pathRel, view, cache );

                        nextPath: for ( final Entry<Neo4jGraphPath, GraphPathInfo> entry : map.entrySet() )
                        {
                            Neo4jGraphPath viewPath = entry.getKey();
                            GraphPathInfo viewPathInfo = entry.getValue();

                            for ( final Long id : cyclicPath )
                            {
                                // only run up to the injector...partial path iteration if the startRid isn't the first in the cycle.
                                if ( id == injector.getId() )
                                {
                                    continue nextPath;
                                }

                                Relationship r = maint.getRelationship( id );

                                r = maint.select( r, view, viewPathInfo, viewPath );

                                if ( r == null )
                                {
                                    continue nextPath;
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
            }

            if ( !found )
            {
                logger.debug( "Cycle is not wholly contained in view: {}", cyclicPath );
                return;
            }
        }

        addCycleInternal( cyclicPath, injector );
    }

    public void addCycle( final CyclePath cyclicPath, final Relationship injector )
    {
        if ( !seenCycles.add( cyclicPath ) )
        {
            logger.debug( "Already seen cycle path: {}", cyclicPath );
            return;
        }

        if ( cyclicPath.length() < 1 )
        {
            logger.debug( "No paths in cycle!" );
            return;
        }

        addCycleInternal( cyclicPath, injector );
    }

    private void addCycleInternal( final CyclePath cyclicPath, final Relationship injector )
    {
        Conversions.storeCachedCyclePath( cyclicPath, viewNode );
    }

    public static CyclePath getTerminatingCycle( final Path path )
    {
        final Logger logger = LoggerFactory.getLogger( CycleCacheUpdater.class );
        logger.debug( "Looking for terminating cycle in: {}", path );

        final List<Long> rids = new ArrayList<Long>();
        final List<Long> starts = new ArrayList<Long>();
        for ( final Relationship pathR : path.relationships() )
        {
            rids.add( pathR.getId() );

            final long sid = pathR.getStartNode()
                                  .getId();

            final long eid = pathR.getEndNode()
                                  .getId();

            final int idx = starts.indexOf( eid );
            if ( idx > -1 )
            {
                final CyclePath cp = new CyclePath( rids.subList( idx, rids.size() - 1 ) );
                logger.debug( "Detected cycle: {}", cp );

                return cp;
            }

            starts.add( sid );
        }

        logger.debug( "No cycle detected" );

        return null;
    }

    public static CyclePath getTerminatingCycle( final Neo4jGraphPath graphPath, final GraphAdmin admin )
    {
        final Logger logger = LoggerFactory.getLogger( CycleCacheUpdater.class );
        logger.debug( "Looking for terminating cycle in: {}", graphPath );

        final Map<Long, Long> startNodesToRids = new HashMap<Long, Long>();
        final long[] rids = graphPath.getRelationshipIds();
        Long startRid = null;
        for ( final long rid : rids )
        {
            final Relationship r = admin.getRelationship( rid );

            final long eid = r.getEndNode()
                              .getId();

            startRid = startNodesToRids.get( eid );
            if ( startRid != null )
            {
                break;
            }

            startNodesToRids.put( r.getStartNode()
                                   .getId(), r.getId() );
        }

        if ( startRid != null )
        {
            int i = 0;
            for ( ; i < rids.length; i++ )
            {
                if ( rids[i] == startRid )
                {
                    break;
                }
            }

            final long[] cycle = new long[rids.length - i];
            System.arraycopy( rids, i, cycle, 0, cycle.length );

            final CyclePath cp = new CyclePath( cycle );
            logger.debug( "Detected cycle: {}", cp );
            return cp;
        }

        logger.debug( "No cycle detected" );

        return null;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setConversionCache( cache );
    }

    public int getCycleCount()
    {
        return cycles.size();
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

}
