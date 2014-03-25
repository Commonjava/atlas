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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getCachedPathInfo;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.accepted;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.AbstractNeo4JEGraphDriver;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.ViewIndexes;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AtlasCollector<STATE>
    implements Evaluator, PathExpander<STATE>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Direction direction = Direction.OUTGOING;

    private final Set<Node> startNodes;

    private GraphView view;

    private ConversionCache cache = new ConversionCache();

    private TraverseVisitor visitor;

    private GraphAdmin admin;

    private ViewIndexes indexes;

    private long traverseId = -1;

    public AtlasCollector( final TraverseVisitor visitor, final Node start, final GraphView view, final ViewIndexes indexes, final GraphAdmin admin )
    {
        this( visitor, Collections.singleton( start ), view, indexes, admin );
    }

    public AtlasCollector( final TraverseVisitor visitor, final Set<Node> startNodes, final GraphView view, final ViewIndexes indexes,
                           final GraphAdmin admin )
    {
        this.visitor = visitor;
        this.indexes = indexes;
        this.admin = admin;
        visitor.configure( this );

        this.startNodes = startNodes;

        this.view = view;
    }

    public AtlasCollector( final TraverseVisitor visitor, final Set<Node> startNodes, final GraphView view, final ViewIndexes indexes,
                           final GraphAdmin admin, final Direction direction )
    {
        this( visitor, startNodes, view, indexes, admin );
        this.direction = direction;
    }

    public void setTraverseId( final long traverseId )
    {
        this.traverseId = traverseId;
    }

    public void setConversionCache( final ConversionCache cache )
    {
        this.cache = cache;
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public final Iterable<Relationship> expand( final Path path, final BranchState state )
    {
        if ( !visitor.isEnabledFor( path ) )
        {
            logger.debug( "Disabled, NOT expanding: {}", path );
            return Collections.emptySet();
        }

        if ( !startNodes.isEmpty() )
        {
            final Node startNode = path.startNode();
            if ( !startNodes.contains( startNode ) )
            {
                logger.debug( "Rejecting path; it does not start with one of our roots:\n\t{}", path );
                return Collections.emptySet();
            }

            for ( final Node node : path.nodes() )
            {
                if ( !node.equals( startNode ) && startNodes.contains( node ) )
                {
                    // TODO: is this safe to discard? I think so, except possibly in rare cases...
                    logger.debug( "Redundant path detected; another start node is contained in path intermediary nodes." );
                }
            }
        }

        final RelationshipIndex cachedPaths = indexes.getCachedPaths();

        Neo4jGraphPath graphPath = new Neo4jGraphPath( path );
        graphPath = visitor.spliceGraphPathFor( graphPath, path );

        GraphPathInfo pathInfo;
        if ( path.lastRelationship() == null )
        {
            pathInfo = new GraphPathInfo( view );
        }
        else
        {
            final IndexHits<Relationship> pathRelHits = cachedPaths.get( RID, path.lastRelationship()
                                                                                  .getId() );

            if ( !pathRelHits.hasNext() )
            {
                return Collections.emptySet();
            }

            final Relationship pathRel = pathRelHits.next();
            pathInfo = getCachedPathInfo( graphPath, pathRel, cache, view );
        }

        logger.debug( "Retrieved pathInfo for {} of: {}", graphPath, pathInfo );

        if ( pathInfo == null )
        {
            if ( path.lastRelationship() == null )
            {
                pathInfo = visitor.initializeGraphPathInfoFor( path, graphPath, view );
                if ( pathInfo == null )
                {
                    logger.debug( "Failed to initialize path info for: {}", path );
                    return Collections.emptySet();
                }
                else
                {
                    logger.debug( "Initialized pathInfo to: {} for path: {}", pathInfo, path );
                }
            }
            else
            {
                logger.debug( "path has at least one relationship but no associated pathInfo: {}", path );
                return Collections.emptySet();
            }
        }

        pathInfo = visitor.spliceGraphPathInfoFor( pathInfo, graphPath, path );

        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( path );
        if ( cyclePath != null )
        {
            final Relationship injector = path.lastRelationship();
            logger.debug( "Detected cycle in progress for path: {} at relationship: {}\n  Cycle path is: {}", path, injector, cyclePath );

            visitor.cycleDetected( cyclePath, injector );
        }

        //        logger.debug( "Checking hasSeen for graphPath: {} with pathInfo: {} (actual path: {})", graphPath, pathInfo, path );
        //        if ( visitor.hasSeen( graphPath, pathInfo ) )
        //        {
        //            logger.debug( "Already seen: {} (path: {})", graphPath, path );
        //            return Collections.emptySet();
        //        }
        // split this so we register both the seen and the cycle.
        /*else*/if ( cyclePath != null )
        {
            return Collections.emptySet();
        }

        if ( returnChildren( path, graphPath, pathInfo ) )
        {

            //            final ProjectRelationshipFilter nextFilter = pathInfo.getFilter();
            //            log( "Implementation says return the children of: {}\n  lastRel={}\n  nextFilter={}\n\n",
            //                 path.endNode()
            //                     .hasProperty( GAV ) ? path.endNode()
            //                                               .getProperty( GAV ) : "Unknown", path.lastRelationship(), nextFilter );

            final Set<Relationship> nextRelationships = new HashSet<Relationship>();

            logger.debug( "Getting relationships from node: {} ({}) in direction: {} (path: {})", path.endNode(), path.endNode()
                                                                                                                      .getProperty( GAV ), direction,
                          path );
            final Iterable<Relationship> relationships = path.endNode()
                                                             .getRelationships( direction );

            //            logger.info( "{} Determining which of {} child relationships to expand traversal into for: {}\n{}", getClass().getName(), path.length(),
            //                         path.endNode()
            //                             .hasProperty( GAV ) ? path.endNode()
            //                                                       .getProperty( GAV ) : "Unknown", new JoinString( "\n  ", Thread.currentThread()
            //                                                                                                                      .getStackTrace() ) );

            final RelationshipIndex toExtendPaths = indexes.getToExtendPaths( traverseId );
            for ( Relationship r : relationships )
            {
                final AbstractNeo4JEGraphDriver db = (AbstractNeo4JEGraphDriver) view.getDatabase();
                logger.debug( "Using database: {} to check selection of: {} in path: {}", db, wrap( r ), path );

                final Relationship selected = db == null ? null : db.select( r, view, pathInfo, graphPath );
                if ( selected == null )
                {
                    logger.debug( "selection failed for: {} at {}. Likely, this is filter rejection from: {}", r, graphPath, pathInfo );
                    continue;
                }

                // if no selection happened and r is a selection-only relationship, skip it.
                if ( selected == r && admin.isSelection( r, view ) )
                {
                    logger.debug( "{} is NOT the result of selection, yet it is marked as a selection relationship. Path: {}", r, path );
                    continue;
                }

                if ( !accepted( selected, view, cache ) )
                {
                    logger.debug( "{} NOT accepted, likely due to incompatible POM location or source URI. Path: {}", r, path );
                    continue;
                }

                if ( selected != null )
                {
                    r = selected;
                }

                final ProjectRelationship<?> rel = toProjectRelationship( r, cache );

                final Neo4jGraphPath nextPath = new Neo4jGraphPath( graphPath, r.getId() );
                GraphPathInfo nextPathInfo = pathInfo.getChildPathInfo( rel );

                // allow for cases where we're bootstrapping the pathInfos map before the traverse starts.
                if ( path.lastRelationship() == null )
                {
                    final IndexHits<Relationship> hits = toExtendPaths.get( RID, r.getId() );
                    if ( hits.hasNext() )
                    {
                        nextPathInfo = getCachedPathInfo( nextPath, hits.next(), cache, view );
                        logger.debug( "Bootstrapped resumed path: {} to use pathInfo: {}", nextPath, nextPathInfo );
                    }
                }

                logger.debug( "Including child: {} with next-path: {} and childPathInfo: {} from parent path: {}", r, nextPath, nextPathInfo, path );
                visitor.includingChild( r, nextPath, nextPathInfo, path );

                logger.debug( "+= {}", wrap( r ) );
                nextRelationships.add( r );
            }

            return nextRelationships;
        }

        logger.debug( "children not being returned for: {}", path );
        return Collections.emptySet();
    }

    public boolean returnChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        // if there's a GraphPathInfo mapped for this path, then it was accepted during expansion.
        return visitor.includeChildren( path, graphPath, pathInfo );
    }

    private Object wrap( final Relationship r )
    {
        return new Object()
        {
            @Override
            public String toString()
            {
                return r + " " + String.valueOf( toProjectRelationship( r, cache ) );
            }
        };
    }

    @Override
    public final Evaluation evaluate( final Path path )
    {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    @Override
    public PathExpander<STATE> reverse()
    {
        final AtlasCollector<STATE> collector = new AtlasCollector<STATE>( visitor, startNodes, view, indexes, admin, direction.reverse() );
        //        collector.setPathInfoMap( pathInfos );
        collector.setConversionCache( cache );
        collector.setTraverseId( traverseId );

        return collector;
    }

}
