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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

@SuppressWarnings( "rawtypes" )
public class CycleDetectingCollector
    implements AtlasCollector<Map.Entry<ProjectRelationship<?>, Set<List<Relationship>>>>
{

    //    private final Logger logger = new Logger( getClass() );

    private final Map<NodePair, ProjectRelationship<?>> map;

    private final Direction direction;

    private final Map<ProjectRelationship<?>, Set<List<Relationship>>> found = new HashMap<ProjectRelationship<?>, Set<List<Relationship>>>();

    private final Set<Long> seen = new HashSet<Long>();

    private final Set<Node> startNodes;

    private final Set<Node> endNodes;

    //    private final GraphView view;

    public CycleDetectingCollector( final Map<NodePair, ProjectRelationship<?>> map/*, final GraphView view*/)
    {
        this.map = map;
        //        this.view = view;
        this.direction = Direction.OUTGOING;

        final Set<Node> start = new HashSet<Node>();
        final Set<Node> end = new HashSet<Node>();
        for ( final NodePair pair : map.keySet() )
        {
            start.add( pair.getFrom() );
            end.add( pair.getTo() );
        }

        this.startNodes = start;
        this.endNodes = end;
    }

    private CycleDetectingCollector( final Map<NodePair, ProjectRelationship<?>> map, final Set<Node> startNodes, final Set<Node> endNodes,
    /*final GraphView view,*/final Direction direction )
    {
        this.map = map;
        this.startNodes = startNodes;
        this.endNodes = endNodes;
        //        this.view = view;
        this.direction = direction;
    }

    @Override
    public PathExpander reverse()
    {
        return new CycleDetectingCollector( map, startNodes, endNodes, /*view,*/direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Map<ProjectRelationship<?>, Set<List<Relationship>>> getFoundPathMap()
    {
        return found;
    }

    @Override
    public Iterator<Map.Entry<ProjectRelationship<?>, Set<List<Relationship>>>> iterator()
    {
        return found.entrySet()
                    .iterator();
    }

    protected boolean returnChildren( final Path path )
    {
        if ( path.length() < 1 )
        {
            return true;
        }

        final Node end = path.endNode();
        if ( !endNodes.contains( end ) )
        {
            return true;
        }

        final Node start = path.startNode();

        //        logger.info( "Checking path: %s from: %s to: %s for cycle trigger.", path, start.getId(), end.getId() );

        final NodePair pair = new NodePair( start, end );
        final ProjectRelationship<?> rel = map.get( pair );

        if ( rel == null )
        {
            //            logger.info( "No cycle. Continue traversal." );
            return true;
        }

        addCycle( pair, rel, path.relationships() );

        return false;
    }

    private void addCycle( final NodePair pair, final ProjectRelationship<?> rel, final Iterable<Relationship> path )
    {
        //        logger.info( "CYCLE!!! Logging: %s for: %s", path, rel );
        Set<List<Relationship>> paths;
        synchronized ( map )
        {
            paths = found.get( rel );
            if ( paths == null )
            {
                paths = new HashSet<List<Relationship>>();
                found.put( rel, paths );
            }
        }

        final List<Relationship> rels = new ArrayList<Relationship>();
        for ( final Relationship r : path )
        {
            //            if ( !TraversalUtils.acceptedInView( r, view ) )
            //            {
            //                return;
            //            }

            rels.add( r );
        }

        paths.add( rels );
    }

    @Override
    public final Iterable<Relationship> expand( final Path path, final BranchState state )
    {
        final Node startNode = path.startNode();
        if ( !startNodes.isEmpty() && !startNodes.contains( path.startNode() ) )
        {
            //            logger.info( "Rejecting path; it does not start with one of our roots:\n\t%s", path );
            return Collections.emptySet();
        }

        final Relationship lastRelationship = path.lastRelationship();
        if ( lastRelationship != null )
        {
            // NOTE: Have to use relationshipId, because multiple relationships may exist between any two GAVs.
            // Most common is managed and unmanaged flavors of the same basic relationship (eg. dependencies).
            final Long endId = lastRelationship.getId();

            if ( seen.contains( endId ) )
            {
                //                logger.info( "Rejecting path; already seen it:\n\t%s", path );
                return Collections.emptySet();
            }

            seen.add( endId );
        }

        if ( returnChildren( path ) )
        {
            //            logger.info( "Returning children of path: %s from end-node: %s", path, path.endNode() );

            final Iterable<Relationship> relationships = path.endNode()
                                                             .getRelationships( direction );

            final Set<Relationship> expansion = new HashSet<Relationship>();

            Node endNode;
            NodePair pair;
            ProjectRelationship<?> rel;
            List<Relationship> pathRelationships = null;
            for ( final Relationship r : relationships )
            {
                endNode = r.getEndNode();
                pair = new NodePair( startNode, endNode );

                rel = map.get( pair );
                if ( rel != null )
                {
                    if ( pathRelationships == null )
                    {
                        pathRelationships = new ArrayList<Relationship>();
                        for ( final Relationship pr : path.relationships() )
                        {
                            pathRelationships.add( pr );
                        }
                    }

                    final List<Relationship> rels = new ArrayList<Relationship>( pathRelationships );
                    rels.add( r );
                    addCycle( pair, rel, rels );
                }
                else
                {
                    //                    logger.info( "Expanding path: %s with included relationship: %s from: %s to: %s", path, r, r.getStartNode(), r.getEndNode() );
                    expansion.add( r );
                }
            }

            return expansion;
        }

        //        logger.info( "NOT expanding: %s", path );
        return Collections.emptySet();
    }

    @Override
    public final Evaluation evaluate( final Path path )
    {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }
}
