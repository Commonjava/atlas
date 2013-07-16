package org.commonjava.maven.atlas.effective.workspace;

import org.commonjava.maven.atlas.common.ref.ProjectRef;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.common.version.SingleVersion;
import org.commonjava.maven.atlas.spi.GraphDriverException;

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
