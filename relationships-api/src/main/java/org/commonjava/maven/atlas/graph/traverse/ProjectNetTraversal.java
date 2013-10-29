/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.List;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;

public interface ProjectNetTraversal
{

    TraversalType getType( int pass );

    int getRequiredPasses();

    void startTraverse( int pass )
        throws GraphDriverException;

    void endTraverse( int pass )
        throws GraphDriverException;

    boolean traverseEdge( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    void edgeTraversed( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    boolean preCheck( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    boolean isStopped();

    TraversalType[] getTraversalTypes();
}
