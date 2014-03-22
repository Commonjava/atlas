package org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;

public class MemorySeenTracker
    implements TraverseSeenTracker
{

    private final Set<String> seenKeys = new HashSet<String>();

    @Override
    public boolean hasSeen( final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        final String key = graphPath.getKey() + "#" + pathInfo.getKey();
        return !seenKeys.add( key );
    }

}
