package org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;

public class CycleAwareMemorySeenTracker
    implements TraverseSeenTracker
{

    private final Set<String> seenKeys = new HashSet<String>();

    private final GraphAdmin admin;

    public CycleAwareMemorySeenTracker( final GraphAdmin admin )
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

}
