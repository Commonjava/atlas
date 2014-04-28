package org.commonjava.maven.atlas.graph;


public interface RelationshipGraphListener
{

    void closing( RelationshipGraph graph );

    void closed( RelationshipGraph graph );

}
