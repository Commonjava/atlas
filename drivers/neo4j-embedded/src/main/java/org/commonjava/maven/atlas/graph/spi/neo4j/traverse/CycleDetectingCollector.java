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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;

@SuppressWarnings( "rawtypes" )
public class CycleDetectingCollector
    extends AbstractAtlasCollector<Map.Entry<ProjectRelationship<?>, Set<CyclePath>>>
{

    //    private final Logger logger = new Logger( getClass() );

    private final Map<NodePair, ProjectRelationship<?>> map;

    private final Map<ProjectRelationship<?>, Set<CyclePath>> found = new HashMap<ProjectRelationship<?>, Set<CyclePath>>();

    public CycleDetectingCollector( final Set<Node> startNodes, final Map<NodePair, ProjectRelationship<?>> map, final GraphView view )
    {
        super( startNodes, view, false, false );
        this.map = map;
    }

    private CycleDetectingCollector( final Map<NodePair, ProjectRelationship<?>> map, final Set<Node> startNodes,
    /*final GraphView view,*/final GraphView view, final Direction direction )
    {
        super( startNodes, view, false, false, direction );
        this.map = map;
    }

    @Override
    public PathExpander reverse()
    {
        return new CycleDetectingCollector( map, startNodes, view, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Map<ProjectRelationship<?>, Set<CyclePath>> getFoundPathMap()
    {
        return found;
    }

    @Override
    public Iterator<Map.Entry<ProjectRelationship<?>, Set<CyclePath>>> iterator()
    {
        return found.entrySet()
                    .iterator();
    }

    private void addCycle( final NodePair pair, final ProjectRelationship<?> rel, final List<Relationship> path )
    {
        logger.debug( "CYCLE!!! Logging: {} for: {}", path, rel );
        Set<CyclePath> paths;
        synchronized ( map )
        {
            paths = found.get( rel );
            if ( paths == null )
            {
                paths = new HashSet<CyclePath>();
                found.put( rel, paths );
            }
        }

        final List<Long> rels = new ArrayList<Long>();
        for ( final Relationship r : path )
        {
            rels.add( r.getId() );
        }

        paths.add( new CyclePath( rels ) );
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        final Relationship last = path.lastRelationship();
        if ( last == null )
        {
            return true;
        }

        final Relationship first = path.relationships()
                                       .iterator()
                                       .next();
        if ( first == last && first.getType() == GraphRelType.PARENT )
        {
            return true;
        }

        final Node startNode = path.startNode();
        final Node endNode = path.endNode();
        if ( endNode.getId() == startNode.getId() )
        {
            // this relationship completes a cycle, so we need to pair the 
            // cycle start-node with this RELATIONSHIP's start-node in order
            // to lookup the node-pair and get the ProjectRelationship<?> associated.

            // TODO: We could just read the ProjectRelationship<?> directly from this relationship...
            final NodePair pair = new NodePair( endNode, last.getStartNode() );

            final ProjectRelationship<?> rel = map.get( pair );
            if ( rel != null )
            {
                final List<Relationship> cycleRels = new ArrayList<Relationship>();
                for ( final Relationship r : path.relationships() )
                {
                    cycleRels.add( r );
                }

                addCycle( pair, rel, cycleRels );
                return false;
            }
        }

        return true;
    }
}
