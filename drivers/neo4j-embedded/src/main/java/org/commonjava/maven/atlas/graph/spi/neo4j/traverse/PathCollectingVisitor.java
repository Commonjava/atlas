/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
