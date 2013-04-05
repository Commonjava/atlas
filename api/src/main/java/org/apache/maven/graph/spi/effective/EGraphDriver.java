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
package org.apache.maven.graph.spi.effective;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;

public interface EGraphDriver
    extends Closeable
{

    EGraphDriver newInstance()
        throws GraphDriverException;

    void reindex()
        throws GraphDriverException;

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships();

    Set<List<ProjectRelationship<?>>> getAllPathsTo( ProjectVersionRef... projectVersionRefs );

    /**
     * Add the given relationships. Skip/return those that introduce cycles.
     * 
     * @return The set of relationships that were NOT added because they introduce cycles. NEVER null, but maybe empty.
     */
    Set<ProjectRelationship<?>> addRelationships( ProjectRelationship<?>... rel );

    boolean introducesCycle( ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects();

    void traverse( ProjectNetTraversal traversal, EProjectNet net, ProjectVersionRef root )
        throws GraphDriverException;

    boolean containsProject( ProjectVersionRef ref );

    boolean containsRelationship( ProjectRelationship<?> rel );

    boolean isDerivedFrom( EGraphDriver driver );

    boolean isMissing( ProjectVersionRef project );

    boolean hasMissingProjects();

    Set<ProjectVersionRef> getMissingProjects();

    boolean hasVariableProjects();

    Set<ProjectVersionRef> getVariableProjects();

    boolean addCycle( EProjectCycle cycle );

    Set<EProjectCycle> getCycles();

    boolean isCycleParticipant( ProjectRelationship<?> rel );

    boolean isCycleParticipant( ProjectVersionRef ref );

    void recomputeIncompleteSubgraphs();

    Map<String, String> getProjectMetadata( ProjectVersionRef ref );

    void addProjectMetadata( ProjectVersionRef ref, String key, String value );

    void addProjectMetadata( ProjectVersionRef ref, Map<String, String> metadata );

    EGraphDriver newInstanceFrom( EProjectNet net, ProjectRelationshipFilter filter, ProjectVersionRef... refs )
        throws GraphDriverException;

    Set<ProjectVersionRef> getProjectsWithMetadata( String key );

    void selectVersionFor( ProjectVersionRef variable, ProjectVersionRef select )
        throws GraphDriverException;

    Map<ProjectVersionRef, ProjectVersionRef> clearSelectedVersions()
        throws GraphDriverException;

    Map<ProjectVersionRef, ProjectVersionRef> getSelectedVersions()
        throws GraphDriverException;

    Set<ProjectVersionRef> getRoots();

    void addDisconnectedProject( ProjectVersionRef ref );

}
