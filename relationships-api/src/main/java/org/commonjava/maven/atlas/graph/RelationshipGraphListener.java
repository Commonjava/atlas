package org.commonjava.maven.atlas.graph;


public interface RelationshipGraphListener
{

    void closing( RelationshipGraph graph )
        throws RelationshipGraphException;

    void closed( RelationshipGraph graph )
        throws RelationshipGraphException;

}
