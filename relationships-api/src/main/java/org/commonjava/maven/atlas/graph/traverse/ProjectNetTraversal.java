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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;

public interface ProjectNetTraversal
{

    TraversalType getType( int pass );

    int getRequiredPasses();

    void startTraverse( int pass, EProjectNet network )
        throws GraphDriverException;

    void endTraverse( int pass, EProjectNet network )
        throws GraphDriverException;

    boolean traverseEdge( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    void edgeTraversed( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    boolean preCheck( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    TraversalType[] getTraversalTypes();
}
