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
        throws RelationshipGraphConnectionException;

    GraphWorkspace createWorkspace( String id, GraphWorkspaceConfiguration config )
        throws RelationshipGraphConnectionException;

    void storeWorkspace( GraphWorkspace workspace )
        throws RelationshipGraphConnectionException;

    GraphWorkspace loadWorkspace( String id )
        throws RelationshipGraphConnectionException;

    Set<GraphWorkspace> loadAllWorkspaces( Set<String> excludedIds );

}
