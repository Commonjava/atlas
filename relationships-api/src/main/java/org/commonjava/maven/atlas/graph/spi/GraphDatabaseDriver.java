/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.spi;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface GraphDatabaseDriver
    extends Closeable
{

    /* 
     * #########################
     * Mutations are viewless
     * #########################
     */

    boolean addCycle( EProjectCycle cycle )
        throws GraphDriverException;

    void addDisconnectedProject( ProjectVersionRef ref )
        throws GraphDriverException;

    void addMetadata( ProjectVersionRef ref, String key, String value )
        throws GraphDriverException;

    void setMetadata( ProjectVersionRef ref, Map<String, String> metadata )
        throws GraphDriverException;

    void deleteRelationshipsDeclaredBy( ProjectVersionRef root )
        throws GraphDriverException;

    /**
     * Add the given relationships. Skip/return those that introduce cycles.
     * 
     * @return The set of relationships that were NOT added because they introduce cycles. NEVER null, but maybe empty.
     */
    Set<ProjectRelationship<?>> addRelationships( ProjectRelationship<?>... rel )
        throws GraphDriverException;

    void recomputeIncompleteSubgraphs()
        throws GraphDriverException;

    void reindex()
        throws GraphDriverException;

    /* 
     * ################################################
     * Queries require a view
     * ---
     * View param is first to support vararg methods
     * ################################################
     */

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( GraphView view, ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( GraphView view, ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships( GraphView view );

    Set<List<ProjectRelationship<?>>> getAllPathsTo( GraphView view, ProjectVersionRef... projectVersionRefs );

    boolean introducesCycle( GraphView view, ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects( GraphView view );

    void traverse( GraphView view, ProjectNetTraversal traversal, EProjectNet net, ProjectVersionRef root )
        throws GraphDriverException;

    boolean containsProject( GraphView view, ProjectVersionRef ref );

    boolean containsRelationship( GraphView view, ProjectRelationship<?> rel );

    boolean isMissing( GraphView view, ProjectVersionRef project );

    boolean hasMissingProjects( GraphView view );

    Set<ProjectVersionRef> getMissingProjects( GraphView view );

    boolean hasVariableProjects( GraphView view );

    Set<ProjectVersionRef> getVariableProjects( GraphView view );

    Set<EProjectCycle> getCycles( GraphView view );

    boolean isCycleParticipant( GraphView view, ProjectRelationship<?> rel );

    boolean isCycleParticipant( GraphView view, ProjectVersionRef ref );

    Map<String, String> getMetadata( ProjectVersionRef ref );

    Map<String, String> getMetadata( ProjectVersionRef ref, Set<String> keys );

    Set<ProjectVersionRef> getProjectsWithMetadata( GraphView view, String key );

    /**
     * @deprecated Use {@link #getDirectRelationshipsFrom(GraphView,ProjectVersionRef,boolean,boolean,RelationshipType...)} instead
     */
    @Deprecated
    Set<ProjectRelationship<?>> getDirectRelationshipsFrom( GraphView eProjectNetView, ProjectVersionRef from, boolean includeManagedInfo,
                                                            RelationshipType... types );

    /**
     * @deprecated Use {@link #getDirectRelationshipsTo(GraphView,ProjectVersionRef,boolean,boolean,RelationshipType...)} instead
     */
    @Deprecated
    Set<ProjectRelationship<?>> getDirectRelationshipsTo( GraphView eProjectNetView, ProjectVersionRef to, boolean includeManagedInfo,
                                                          RelationshipType... types );

    Set<ProjectRelationship<?>> getDirectRelationshipsFrom( GraphView eProjectNetView, ProjectVersionRef from, boolean includeManagedInfo,
                                                            boolean includeConcreteInfo, RelationshipType... types );

    Set<ProjectRelationship<?>> getDirectRelationshipsTo( GraphView eProjectNetView, ProjectVersionRef to, boolean includeManagedInfo,
                                                          boolean includeConcreteInfo, RelationshipType... types );

    Set<ProjectVersionRef> getProjectsMatching( ProjectRef projectRef, GraphView eProjectNetView );

}
