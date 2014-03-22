package org.commonjava.maven.atlas.graph.spi.neo4j.update;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.CACHED_PATH_TARGETS;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AbstractTraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.neo4j.graphdb.Node;
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

    private final RelationshipIndex cachedPathRels;

    private final GraphView view;

    private final RelationshipIndex cyclePathRels;

    private final Node viewNode;

    private int cycleCount = 0;

    public CycleCacheUpdater( final RelationshipIndex cyclePathRels, final RelationshipIndex cachedPathRels, final GraphView view,
                              final Node viewNode, final GraphAdmin maint, final ConversionCache cache )
    {
        this.cyclePathRels = cyclePathRels;
        this.cachedPathRels = cachedPathRels;
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

                    final Relationship startR = maint.getRelationship( startRid );
                    final long startNid = startR.getStartNode()
                                                .getId();

                    final IndexHits<Relationship> pathHits = cachedPathRels.get( CACHED_PATH_TARGETS, startNid );
                    nextCachedPath: for ( final Relationship pathRel : pathHits )
                    {
                        Neo4jGraphPath viewPath = Conversions.getCachedPath( pathRel );
                        GraphPathInfo viewPathInfo = Conversions.getCachedPathInfo( pathRel, cache, maint );
                        for ( final Long id : cyclicPath )
                        {
                            Relationship r = maint.getRelationship( id );

                            r = maint.select( r, view, viewPathInfo, viewPath );

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
                return;
            }
        }

        final IndexHits<Relationship> injectorHits = cyclePathRels.get( RID, injector.getId() );
        if ( !injectorHits.hasNext() )
        {
            final Relationship cyclePath = viewNode.createRelationshipTo( injector.getStartNode(), GraphRelType.CACHED_CYCLE_RELATIONSHIP );

            cyclePathRels.add( cyclePath, RID, injector.getId() );

            final CyclePath reoriented = cyclicPath.reorientToEntryPoint();
            logger.debug( "Caching cycle: {} for view: {} in path relationship: {} (original was: {})", reoriented, view.getShortId(), cyclePath,
                          cyclicPath );

            Conversions.storeCachedPath( reoriented, null, cyclePath );
            cyclePath.setProperty( RID, injector.getId() );
        }
        else
        {
            logger.debug( "Cycle is already contained in view cache: {}", cyclicPath );
        }

        cycleCount++;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setAvoidCycles( false );
        collector.setConversionCache( cache );
    }

    public int getCycleCount()
    {
        return cycleCount;
    }

}
