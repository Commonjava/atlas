/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.workspace;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractGraphWorkspaceListener
    implements GraphWorkspaceListener
{

    @Override
    public void selectionAdded( final GraphWorkspace ws, final ProjectRef ref, final ProjectVersionRef selected )
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
