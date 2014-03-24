package org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;

public class CycleAwareMemorySeenTracker
    implements TraverseSeenTracker
{

    private final Set<String> seenKeys = new HashSet<String>();

    @Override
    public boolean hasSeen( final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        String key;
        if ( graphPath.containsCycle() )
        {
            final CyclePath cyclePath = graphPath instanceof CyclePath ? (CyclePath) graphPath : new CyclePath( graphPath.getRelationshipIds() );
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
