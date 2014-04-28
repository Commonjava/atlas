package org.commonjava.maven.atlas.graph;

import java.util.Set;

public interface RelationshipGraphFactory
{

    RelationshipGraph open( ViewParams params, boolean create )
        throws RelationshipGraphException;

    Set<String> listWorkspaces();

    void deleteWorkspace( String workspaceId );

}
