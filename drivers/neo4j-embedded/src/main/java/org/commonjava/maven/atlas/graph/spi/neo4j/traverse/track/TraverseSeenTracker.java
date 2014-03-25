package org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;

public interface TraverseSeenTracker
{

    boolean hasSeen( Neo4jGraphPath graphPath, GraphPathInfo pathInfo );

    void traverseComplete();

}
