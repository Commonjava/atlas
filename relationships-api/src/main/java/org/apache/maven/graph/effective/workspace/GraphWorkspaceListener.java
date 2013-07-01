package org.apache.maven.graph.effective.workspace;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.GraphWorkspace;
import org.apache.maven.graph.spi.GraphDriverException;

public interface GraphWorkspaceListener
{

    void selectionAdded( GraphWorkspace session, ProjectVersionRef ref, SingleVersion version )
        throws GraphDriverException;

    void sessionClosed( GraphWorkspace session )
        throws GraphDriverException;

    void selectionsCleared( GraphWorkspace session )
        throws GraphDriverException;
}
