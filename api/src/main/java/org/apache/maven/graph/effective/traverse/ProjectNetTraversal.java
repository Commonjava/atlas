/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective.traverse;

import java.util.List;

import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.spi.GraphDriverException;

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
