package org.commonjava.maven.atlas.spi.neo4j.io;

import org.neo4j.graphdb.Relationship;

public class RelationshipIdProjector
    implements Projector<Relationship, Long>
{

    public Long project( final Relationship item )
    {
        return item.getId();
    }

}
