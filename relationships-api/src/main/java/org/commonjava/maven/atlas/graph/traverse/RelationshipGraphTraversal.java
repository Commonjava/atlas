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

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;

public interface RelationshipGraphTraversal
{

    void startTraverse( RelationshipGraph graph )
        throws RelationshipGraphConnectionException;

    void endTraverse( RelationshipGraph graph )
        throws RelationshipGraphConnectionException;

    boolean traverseEdge( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path );

    void edgeTraversed( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path );

    boolean preCheck( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path );
}
