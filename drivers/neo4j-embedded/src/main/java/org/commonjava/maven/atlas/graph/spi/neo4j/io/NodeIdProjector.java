package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import org.neo4j.graphdb.Node;

public class NodeIdProjector
    implements Projector<Node, Long>
{

    public Long project( final Node item )
    {
        return item.getId();
    }

}
