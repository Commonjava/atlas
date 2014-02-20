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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;

@SuppressWarnings( "rawtypes" )
public class EndGAVsPathsCollector
    extends AbstractAtlasCollector<Path>
{

    private final Set<ProjectVersionRef> endRefs;

    public EndGAVsPathsCollector( final Node start, final ProjectVersionRef end, final GraphView view, final boolean checkExistence )
    {
        this( Collections.singleton( start ), Collections.singleton( end ), view, checkExistence );
    }

    public EndGAVsPathsCollector( final Set<Node> startNodes, final Set<ProjectVersionRef> endRefs, final GraphView view, final boolean checkExistence )
    {
        super( startNodes, view, checkExistence );
        this.endRefs = endRefs;
    }

    private EndGAVsPathsCollector( final Set<Node> startNodes, final Set<ProjectVersionRef> endRefs, final GraphView view,
                                   final boolean checkExistence, final Direction direction )
    {
        super( startNodes, view, checkExistence, direction );
        this.endRefs = endRefs;
    }

    @Override
    public PathExpander reverse()
    {
        return new EndGAVsPathsCollector( startNodes, endRefs, view, checkExistence, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Set<Path> getFoundPaths()
    {
        return found;
    }

    @Override
    public Iterator<Path> iterator()
    {
        return found.iterator();
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        final Node end = path.endNode();
        if ( !end.hasProperty( Conversions.GAV ) )
        {
            return false;
        }

        final ProjectVersionRef ref = Conversions.toProjectVersionRef( end );
        if ( endRefs.contains( ref ) )
        {
            if ( accept( path ) )
            {
                found.add( path );
            }

            return false;
        }

        return true;
    }

}
