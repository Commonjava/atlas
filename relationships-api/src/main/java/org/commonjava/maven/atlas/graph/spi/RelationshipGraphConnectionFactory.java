package org.commonjava.maven.atlas.graph.spi;

import java.util.Set;

public interface RelationshipGraphConnectionFactory
{

    RelationshipGraphConnection openConnection( String workspaceId, boolean create )
        throws RelationshipGraphConnectionException;

    Set<String> listWorkspaces();

    void flush( RelationshipGraphConnection connection )
        throws RelationshipGraphConnectionException;

    boolean delete( String workspaceId )
        throws RelationshipGraphConnectionException;

    void close()
        throws RelationshipGraphConnectionException;

    boolean exists( String workspaceId );

}
