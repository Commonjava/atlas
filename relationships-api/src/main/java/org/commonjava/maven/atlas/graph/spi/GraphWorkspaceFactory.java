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

    GraphWorkspace createWorkspace( String id, GraphWorkspaceConfiguration config )
        throws GraphDriverException;

    void storeWorkspace( GraphWorkspace workspace )
        throws GraphDriverException;

    GraphWorkspace loadWorkspace( String id )
        throws GraphDriverException;

    Set<GraphWorkspace> loadAllWorkspaces( Set<String> excludedIds );

}
