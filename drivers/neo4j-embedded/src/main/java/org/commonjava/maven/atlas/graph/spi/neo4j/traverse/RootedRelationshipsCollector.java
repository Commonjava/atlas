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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;

@SuppressWarnings( "rawtypes" )
public class RootedRelationshipsCollector
    extends AbstractAtlasCollector<Relationship>
{

    public RootedRelationshipsCollector( final Node start, final GraphView view, final boolean checkExistence )
    {
        super( start, view, checkExistence );
        //        logEnabled = true;
    }

    public RootedRelationshipsCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence )
    {
        super( startNodes, view, checkExistence );
        //        this.logEnabled = true;
    }

    private RootedRelationshipsCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence, final Direction direction )
    {
        super( startNodes, view, checkExistence, direction );
        //        this.logEnabled = true;
    }

    @Override
    public PathExpander reverse()
    {
        return new RootedRelationshipsCollector( startNodes, view, checkExistence, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Set<Relationship> getFoundRelationships()
    {
        return found;
    }

    @Override
    public Iterator<Relationship> iterator()
    {
        return found.iterator();
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        if ( accept( path ) )
        {
            for ( final Relationship r : path.relationships() )
            {
                found.add( r );
            }

            return true;
        }

        return false;
    }

}
