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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;

@SuppressWarnings( "rawtypes" )
public class RootedPathsCollector
    extends AbstractAtlasCollector<Entry<Neo4jGraphPath, GraphPathInfo>>
{

    public RootedPathsCollector( final Node start, final GraphView view )
    {
        super( start, view, false, true );
        setAvoidCycleRelationships( false );
        //        logEnabled = true;
    }

    public RootedPathsCollector( final Set<Node> startNodes, final GraphView view )
    {
        super( startNodes, view, false, true );
        setAvoidCycleRelationships( false );
        //        this.logEnabled = true;
    }

    private RootedPathsCollector( final Set<Node> startNodes, final GraphView view, final Direction direction )
    {
        super( startNodes, view, false, true, direction );
        setAvoidCycleRelationships( false );
        //        this.logEnabled = true;
    }

    public RootedPathsCollector( final Set<Node> startNodes, final Map<Neo4jGraphPath, GraphPathInfo> pathInfos, final GraphView view )
    {
        super( startNodes, view, false, true );
        setAvoidCycleRelationships( false );
        setPathInfoMap( pathInfos );
    }

    @Override
    public PathExpander reverse()
    {
        return new RootedPathsCollector( startNodes, view, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !getPathInfoMap().isEmpty();
    }

    public Set<Entry<Neo4jGraphPath, GraphPathInfo>> getFoundRelationships()
    {
        return getPathInfoMap().entrySet();
    }

    @Override
    public Iterator<Entry<Neo4jGraphPath, GraphPathInfo>> iterator()
    {
        return getPathInfoMap().entrySet()
                               .iterator();
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        logger.debug( "checking return-children for: {}", path );
        return accept( path );
    }

}
