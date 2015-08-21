package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Relationship;

/**
 * Created by jdcasey on 8/20/15.
 */
public class RelToString
{
    private Relationship rel;

    private final ConversionCache cache;

    public RelToString( Relationship rel, ConversionCache cache )
    {
        this.rel = rel;
        this.cache = cache;
    }

    public String toString()
    {
        return Conversions.toProjectRelationship( rel, cache ).toString();
    }
}
