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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;

public class MemorySeenTracker
    implements TraverseSeenTracker
{

    private final Set<String> seenKeys = new HashSet<String>();

    private final GraphAdmin admin;

    public MemorySeenTracker( final GraphAdmin admin )
    {
        this.admin = admin;
    }

    @Override
    public boolean hasSeen( final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        // TODO: This trims the path leading up to the cycle...is that alright??

        String key;
        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( graphPath, admin );
        if ( cyclePath != null )
        {
            key = cyclePath.getKey();
        }
        else
        {
            key = graphPath.getKey();
        }

        key += "#" + pathInfo.getKey();
        return !seenKeys.add( key );
    }

    @Override
    public void traverseComplete()
    {
        seenKeys.clear();
    }

}
