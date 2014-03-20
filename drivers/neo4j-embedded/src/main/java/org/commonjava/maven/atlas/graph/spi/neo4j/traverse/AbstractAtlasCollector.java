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
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.accepted;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.AbstractNeo4JEGraphDriver;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAtlasCollector<T>
    implements AtlasCollector<T>
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    protected boolean logEnabled = false;

    protected Direction direction = Direction.OUTGOING;

    protected final Set<Node> startNodes;

    protected final Set<T> found = new HashSet<T>();

    protected final boolean checkExistence;

    protected GraphView view;

    private final Set<String> seenKeys = new HashSet<String>();

    // FIXME: Optionally, allow these to accumulate for caching in the db later...
    protected Map<Neo4jGraphPath, GraphPathInfo> pathInfos = new HashMap<Neo4jGraphPath, GraphPathInfo>();

    private boolean accumulatePathInfos;

    private boolean avoidCycleRelationships = true;

    protected AbstractAtlasCollector( final Node start, final GraphView view, final boolean checkExistence, final boolean accumulatePathInfos )
    {
        this( Collections.singleton( start ), view, checkExistence, accumulatePathInfos );
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence,
                                      final boolean accumulatePathInfos )
    {
        this.startNodes = startNodes;
        this.view = view;
        this.checkExistence = checkExistence;
        this.accumulatePathInfos = accumulatePathInfos;
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence,
                                      final boolean accumulatePathInfos, final Direction direction )
    {
        this( startNodes, view, checkExistence, accumulatePathInfos );
        this.direction = direction;
    }

    protected AbstractAtlasCollector( final Node start, final GraphView view, final boolean checkExistence )
    {
        this( Collections.singleton( start ), view, checkExistence, false );
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence )
    {
        this( startNodes, view, checkExistence, false );
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence, final Direction direction )
    {
        this( startNodes, view, checkExistence, false, direction );
    }

    protected final void setPathInfoMap( final Map<Neo4jGraphPath, GraphPathInfo> pathInfos )
    {
        this.pathInfos = pathInfos;
    }

    public Map<Neo4jGraphPath, GraphPathInfo> getPathInfoMap()
    {
        return pathInfos;
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public final Iterable<Relationship> expand( final Path path, final BranchState state )
    {
        if ( checkExistence && !found.isEmpty() )
        {
            log( "Only checking for existence, and already found one. Rejecting: {}", path );
            return Collections.emptySet();
        }

        if ( !startNodes.isEmpty() && !startNodes.contains( path.startNode() ) )
        {
            log( "Rejecting path; it does not start with one of our roots:\n\t{}", path );
            return Collections.emptySet();
        }

        final Neo4jGraphPath graphPath = new Neo4jGraphPath( path );
        GraphPathInfo pathInfo = accumulatePathInfos ? pathInfos.get( graphPath ) : pathInfos.remove( graphPath );

        if ( pathInfo == null )
        {
            if ( path.lastRelationship() == null )
            {
                // just starting out. Initialize the path info.
                pathInfo = new GraphPathInfo( view );
            }
            else
            {
                return Collections.emptySet();
            }
        }

        final String key = graphPath.getKey() + "#" + pathInfo.getKey();
        if ( !seenKeys.add( key ) )
        {
            return Collections.emptySet();
        }

        final Set<Long> starts = new HashSet<Long>();
        for ( final Relationship pathR : path.relationships() )
        {
            if ( starts.contains( pathR.getStartNode()
                                       .getId() ) )
            {
                logger.debug( "Detected cycle in progress for path: {} at relationship: {}", path, pathR );
                return Collections.emptySet();
            }
        }

        if ( returnChildren( path ) )
        {

            final ProjectRelationshipFilter nextFilter = pathInfo.getFilter();
            log( "Implementation says return the children of: {}\n  lastRel={}\n  nextFilter={}\n\n",
                 path.endNode()
                     .hasProperty( GAV ) ? path.endNode()
                                               .getProperty( GAV ) : "Unknown", path.lastRelationship(), nextFilter );

            final Set<Relationship> nextRelationships = new HashSet<Relationship>();
            final Iterable<Relationship> relationships = path.endNode()
                                                             .getRelationships( direction );

            //            logger.info( "{} Determining which of {} child relationships to expand traversal into for: {}\n{}", getClass().getName(), path.length(),
            //                         path.endNode()
            //                             .hasProperty( GAV ) ? path.endNode()
            //                                                       .getProperty( GAV ) : "Unknown", new JoinString( "\n  ", Thread.currentThread()
            //                                                                                                                      .getStackTrace() ) );

            for ( Relationship r : relationships )
            {
                if ( avoidCycleRelationships && Conversions.getBooleanProperty( Conversions.CYCLES_INJECTED, r, false ) )
                {
                    logger.debug( "Detected marked cycle from path: {} in child relationship: {}", path, r );
                    continue;
                }

                final AbstractNeo4JEGraphDriver db = (AbstractNeo4JEGraphDriver) view.getDatabase();

                final Relationship selected = db == null ? null : db.select( r, view, pathInfo, graphPath );
                if ( selected == null )
                {
                    continue;
                }

                // if no selection happened and r is a selection-only relationship, skip it.
                if ( selected == r && Conversions.getBooleanProperty( Conversions.SELECTION, r, false ) )
                {
                    continue;
                }

                if ( selected != null )
                {
                    r = selected;
                }

                nextRelationships.add( r );

                final ProjectRelationship<?> rel = toProjectRelationship( r );
                final GraphPathInfo next = pathInfo.getChildPathInfo( rel );
                pathInfos.put( new Neo4jGraphPath( graphPath, r.getId() ), next );

                log( "+= {} [{}]", logwrapper( r ), next.getFilter() );
            }

            return nextRelationships;
        }

        return Collections.emptySet();
    }

    protected abstract boolean returnChildren( Path path );

    protected boolean accept( final Path path )
    {
        final Relationship r = path.lastRelationship();
        if ( r == null )
        {
            return true;
        }

        // if there's a GraphPathInfo mapped for this path, then it was accepted during expansion.
        // If so, then we just need to verify the workspace allows the pomLocation and source
        // TODO: Can we check the workspace restrictions during expansion??
        if ( pathInfos.containsKey( new Neo4jGraphPath( path ) ) )
        {
            return accepted( r, null, view.getWorkspace() );
        }

        return false;

        //        final boolean accept = accepted( r, info.getFilter(), view.getWorkspace() );
        //
        //        if ( logEnabled )
        //        {
        //            final Set<ProjectVersionRef> gavs = new HashSet<ProjectVersionRef>( startNodes.size() );
        //            for ( final Node node : startNodes )
        //            {
        //                gavs.add( toProjectVersionRef( node ) );
        //            }
        //
        //            log( "Checking acceptance: {} [roots: {}, filter: {}]...{}", logwrapper( r ), gavs, info.getFilter(), accept );
        //        }
        //
        //        return accept;
    }

    private Object logwrapper( final Relationship r )
    {
        return new Object()
        {
            @Override
            public String toString()
            {
                return String.valueOf( toProjectRelationship( r ) );
            }
        };
    }

    @Override
    public final Evaluation evaluate( final Path path )
    {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    protected void log( final String format, final Object... params )
    {
        if ( logEnabled )
        {
            logger.info( format, params );
        }
    }

    protected boolean isAvoidCycleRelationships()
    {
        return avoidCycleRelationships;
    }

    protected void setAvoidCycleRelationships( final boolean avoidCycles )
    {
        this.avoidCycleRelationships = avoidCycles;
    }

}
