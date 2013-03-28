package org.commonjava.maven.atlas.spi.neo4j.io;

public interface Projector<T, P>
{

    P project( T item );

}
