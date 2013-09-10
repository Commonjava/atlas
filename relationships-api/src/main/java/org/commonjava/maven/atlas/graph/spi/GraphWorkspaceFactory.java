package org.commonjava.maven.atlas.graph.spi;

import java.io.IOException;
import java.util.Set;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;

public interface GraphWorkspaceFactory
{

    boolean deleteWorkspace( String id )
        throws IOException;

    GraphWorkspace createWorkspace( GraphWorkspaceConfiguration config )
        throws GraphDriverException;

    void storeWorkspace( GraphWorkspace workspace )
        throws GraphDriverException;

    GraphWorkspace loadWorkspace( String id )
        throws GraphDriverException;

    Set<GraphWorkspace> loadAllWorkspaces( Set<String> excludedIds );

}
