/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
