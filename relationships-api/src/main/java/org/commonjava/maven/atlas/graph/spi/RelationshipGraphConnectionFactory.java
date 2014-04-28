package org.commonjava.maven.atlas.graph.spi;

public interface RelationshipGraphConnectionFactory
{

    RelationshipGraphConnection openConnection( String workspaceId );

}
