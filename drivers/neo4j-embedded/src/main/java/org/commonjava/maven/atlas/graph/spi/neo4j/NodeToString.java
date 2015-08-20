package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.neo4j.graphdb.Node;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectVersionRef;

/**
 * Created by jdcasey on 8/20/15.
 */
public class NodeToString
{
    private final Node node;

    private final ConversionCache cache;

    public NodeToString( Node node, ConversionCache cache )
    {
        this.node = node;
        this.cache = cache;
    }

    public String toString()
    {
        return toProjectVersionRef( node, cache ).toString();
    }
}
