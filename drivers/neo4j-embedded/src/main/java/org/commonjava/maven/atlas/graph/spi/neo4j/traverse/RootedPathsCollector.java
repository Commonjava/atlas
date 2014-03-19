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

import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;

@SuppressWarnings( "rawtypes" )
public class RootedPathsCollector
    extends AbstractAtlasCollector<Neo4jGraphPath>
{

    public RootedPathsCollector( final Node start, final GraphView view, final boolean checkExistence )
    {
        super( start, view, checkExistence, true );
        //        logEnabled = true;
    }

    public RootedPathsCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence )
    {
        super( startNodes, view, checkExistence, true );
        //        this.logEnabled = true;
    }

    private RootedPathsCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence, final Direction direction )
    {
        super( startNodes, view, checkExistence, true, direction );
        //        this.logEnabled = true;
    }

    @Override
    public PathExpander reverse()
    {
        return new RootedPathsCollector( startNodes, view, checkExistence, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Set<Neo4jGraphPath> getFoundRelationships()
    {
        return found;
    }

    @Override
    public Iterator<Neo4jGraphPath> iterator()
    {
        return found.iterator();
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        if ( accept( path ) )
        {
            return found.add( new Neo4jGraphPath( path ) );
        }

        return false;
    }

}
