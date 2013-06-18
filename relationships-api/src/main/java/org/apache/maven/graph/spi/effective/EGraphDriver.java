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
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.session.EGraphSessionConfiguration;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;

public interface EGraphDriver
    extends Closeable
{

    /* 
     * #########################
     * Mutations are viewless
     * #########################
     */

    boolean addCycle( EProjectCycle cycle );

    void addDisconnectedProject( ProjectVersionRef ref );

    void addMetadata( ProjectVersionRef ref, String key, String value );

    void addMetadata( ProjectVersionRef ref, Map<String, String> metadata );

    /**
     * Add the given relationships. Skip/return those that introduce cycles.
     * 
     * @return The set of relationships that were NOT added because they introduce cycles. NEVER null, but maybe empty.
     */
    Set<ProjectRelationship<?>> addRelationships( ProjectRelationship<?>... rel );

    void clearSelectedVersionsFor( String id );

    void deRegisterSession( String id );

    String registerNewSession( EGraphSessionConfiguration config )
        throws GraphDriverException;

    void recomputeIncompleteSubgraphs()
        throws GraphDriverException;

    void reindex()
        throws GraphDriverException;

    void selectVersionFor( ProjectVersionRef ref, SingleVersion version, String id );

    /* 
     * ################################################
     * Queries require a view
     * ---
     * View param is first to support vararg methods
     * ################################################
     */

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( EProjectNetView view,
                                                                             ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( EProjectNetView view, ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships( EProjectNetView view );

    Set<List<ProjectRelationship<?>>> getAllPathsTo( EProjectNetView view, ProjectVersionRef... projectVersionRefs );

    boolean introducesCycle( EProjectNetView view, ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects( EProjectNetView view );

    void traverse( EProjectNetView view, ProjectNetTraversal traversal, EProjectNet net, ProjectVersionRef root )
        throws GraphDriverException;

    boolean containsProject( EProjectNetView view, ProjectVersionRef ref );

    boolean containsRelationship( EProjectNetView view, ProjectRelationship<?> rel );

    boolean isMissing( EProjectNetView view, ProjectVersionRef project );

    boolean hasMissingProjects( EProjectNetView view );

    Set<ProjectVersionRef> getMissingProjects( EProjectNetView view );

    boolean hasVariableProjects( EProjectNetView view );

    Set<ProjectVersionRef> getVariableProjects( EProjectNetView view );

    Set<EProjectCycle> getCycles( EProjectNetView view );

    boolean isCycleParticipant( EProjectNetView view, ProjectRelationship<?> rel );

    boolean isCycleParticipant( EProjectNetView view, ProjectVersionRef ref );

    Map<String, String> getMetadata( ProjectVersionRef ref );

    //    EGraphDriver newInstanceFrom( EProjectNet net, ProjectRelationshipFilter filter, ProjectVersionRef... refs )
    //        throws GraphDriverException;
    //
    //    EGraphDriver newInstance( EGraphSession session, EProjectNet net, ProjectRelationshipFilter filter,
    //                              ProjectVersionRef... refs )
    //        throws GraphDriverException;

    Set<ProjectVersionRef> getProjectsWithMetadata( EProjectNetView view, String key );

}
