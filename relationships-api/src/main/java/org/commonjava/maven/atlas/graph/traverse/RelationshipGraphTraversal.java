/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    boolean traverseEdge( ProjectRelationship<?, ?> relationship, List<ProjectRelationship<?, ?>> path );

    void edgeTraversed( ProjectRelationship<?, ?> relationship, List<ProjectRelationship<?, ?>> path );

    boolean preCheck( ProjectRelationship<?, ?> relationship, List<ProjectRelationship<?, ?>> path );
}
