package org.commonjava.maven.atlas.graph.spi.neo4j.io;

public interface Projector<T, P>
{

    P project( T item );

}
