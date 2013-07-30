package org.commonjava.maven.atlas.graph.workspace;

import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

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
