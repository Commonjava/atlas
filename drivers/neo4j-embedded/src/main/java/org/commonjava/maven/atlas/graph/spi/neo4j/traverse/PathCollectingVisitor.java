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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

public class PathCollectingVisitor
    extends AbstractTraverseVisitor
    implements Iterable<Neo4jGraphPath>
{

    private final Set<Node> ends;

    private final Set<Neo4jGraphPath> paths = new HashSet<Neo4jGraphPath>();

    private final ConversionCache cache;

    public PathCollectingVisitor( final Set<Node> ends, final ConversionCache cache )
    {
        this.ends = ends;
        this.cache = cache;
    }

    public Set<Neo4jGraphPath> getPaths()
    {
        return paths;
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        if ( ends.contains( path.endNode() ) )
        {
            paths.add( graphPath );
            return false;
        }

        return true;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setConversionCache( cache );
    }

    @Override
    public Iterator<Neo4jGraphPath> iterator()
    {
        return paths.iterator();
    }

}
