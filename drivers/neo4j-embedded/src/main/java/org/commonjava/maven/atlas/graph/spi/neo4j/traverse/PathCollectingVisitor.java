/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
