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
package org.apache.maven.graph.effective;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public interface EProjectNet
    extends EProjectRelationshipCollection
{

    EGraphDriver getDriver();

    boolean isComplete();

    boolean isConcrete();

    Set<ProjectVersionRef> getIncompleteSubgraphs();

    Set<ProjectVersionRef> getVariableSubgraphs();

    //    void recomputeIncompleteSubgraphs();

    Set<EProjectCycle> getCycles();

    void addCycle( final EProjectCycle cycle );

    boolean isCycleParticipant( final ProjectRelationship<?> rel );

    boolean isCycleParticipant( final ProjectVersionRef ref );

    Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref );

    boolean isDerivedFrom( EProjectNet net );

    EProjectGraph getGraph( EProjectKey key )
        throws GraphDriverException;

    boolean containsGraph( EProjectKey eProjectKey );

    Set<ProjectVersionRef> getAllProjects();

    Set<ProjectRelationship<?>> getAllRelationships();

    Map<String, String> getMetadata( EProjectKey key );

    void addMetadata( EProjectKey key, String name, String value );

    void addMetadata( EProjectKey key, Map<String, String> metadata );

    List<EProjectNet> getSuperNets();

    <T extends ProjectRelationship<?>> Set<T> addAll( Collection<T> rels );
}
