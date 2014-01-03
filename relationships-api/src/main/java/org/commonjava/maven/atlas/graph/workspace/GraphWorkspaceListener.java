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
