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

public interface GraphWorkspaceListener
{

    void selectionAdded( GraphWorkspace ws, ProjectRef ref, ProjectVersionRef selected );

    void closed( GraphWorkspace ws );

    void accessed( GraphWorkspace ws );

    void selectionsCleared( GraphWorkspace ws );

    void detached( GraphWorkspace graphWorkspace );

}
