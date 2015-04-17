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

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public abstract class AbstractTraverseVisitor
    implements TraverseVisitor
{

    private ConversionCache conversionCache;

    public void setConversionCache( final ConversionCache conversionCache )
    {
        this.conversionCache = conversionCache;
    }

    public ConversionCache getConversionCache()
    {
        return conversionCache;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
    }

    @Override
    public boolean isEnabledFor( final Path path )
    {
        return true;
    }

    @Override
    public void cycleDetected( final CyclePath path, final Relationship injector )
    {
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        return true;
    }

    @Override
    public void includingChild( final Relationship child, final Neo4jGraphPath childPath,
                                final GraphPathInfo childPathInfo, final Path parentPath )
    {
    }

    @Override
    public void traverseComplete( final AtlasCollector<?> collector )
    {
    }

}
