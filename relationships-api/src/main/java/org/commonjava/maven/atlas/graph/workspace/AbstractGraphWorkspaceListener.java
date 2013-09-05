package org.commonjava.maven.atlas.graph.workspace;

import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractGraphWorkspaceListener
    implements GraphWorkspaceListener
{

    @Override
    public void selectionAdded( final GraphWorkspace ws, final ProjectVersionRef ref, final ProjectVersionRef selected )
        throws GraphDriverException
    {
    }

    @Override
    public void wildcardSelectionAdded( final GraphWorkspace graphWorkspace, final ProjectRef ref, final ProjectVersionRef selected )
        throws GraphDriverException
    {
    }

    @Override
    public void closed( final GraphWorkspace ws )
    {
    }

    @Override
    public void accessed( final GraphWorkspace ws )
    {
    }

    @Override
    public void selectionsCleared( final GraphWorkspace ws )
    {
    }

    @Override
    public void detached( final GraphWorkspace graphWorkspace )
    {
    }

}
