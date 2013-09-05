package org.commonjava.maven.atlas.graph.workspace;

import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface GraphWorkspaceListener
{

    void selectionAdded( GraphWorkspace ws, ProjectVersionRef ref, ProjectVersionRef selected )
        throws GraphDriverException;

    void wildcardSelectionAdded( GraphWorkspace graphWorkspace, ProjectRef ref, ProjectVersionRef selected )
        throws GraphDriverException;

    void closed( GraphWorkspace ws );

    void accessed( GraphWorkspace ws );

    void selectionsCleared( GraphWorkspace ws );

    void detached( GraphWorkspace graphWorkspace );

}
