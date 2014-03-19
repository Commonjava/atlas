package org.commonjava.maven.atlas.graph.model;

public interface GraphPath<T>
    extends Iterable<T>
{
    String getKey();
}
