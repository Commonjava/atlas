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
