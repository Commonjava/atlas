package org.apache.maven.graph.effective.workspace;

import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.spi.GraphDriverException;

public interface GraphWorkspaceListener
{

    void selectionAdded( GraphWorkspace ws, ProjectVersionRef ref, SingleVersion version )
        throws GraphDriverException;

    void wildcardSelectionAdded( GraphWorkspace graphWorkspace, ProjectRef ref, SingleVersion version )
        throws GraphDriverException;

    void closed( GraphWorkspace ws );

    void accessed( GraphWorkspace ws );

    void selectionsCleared( GraphWorkspace ws );

}
