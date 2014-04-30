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

public abstract class AbstractTraversal
    implements RelationshipGraphTraversal
{


    @Override
    public void startTraverse( final RelationshipGraph graph )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public void endTraverse( final RelationshipGraph graph )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path )
    {
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path )
    {
        return preCheck( relationship, path );
    }
}
