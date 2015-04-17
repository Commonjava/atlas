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
