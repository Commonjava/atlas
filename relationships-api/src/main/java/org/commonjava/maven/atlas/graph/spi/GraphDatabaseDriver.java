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
package org.commonjava.maven.atlas.graph.spi;

import java.io.Closeable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
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

    Set<ProjectVersionRef> getProjectsMatching( GraphView eProjectNetView, ProjectRef projectRef );

    void printStats();

    ProjectVersionRef getManagedTargetFor( ProjectVersionRef target, GraphPath<?> path, RelationshipType type );

    GraphPath<?> createPath( ProjectRelationship<?>... relationships );

    GraphPath<?> createPath( GraphPath<?> parent, ProjectRelationship<?> relationship );

    // Support for GraphWorkspace durable attributes...metadata about the graph as a whole.

    void setLastAccess( long lastAccess );

    long getLastAccess();

    int getActivePomLocationCount();

    void addActivePomLocations( URI... locations );

    void addActivePomLocations( Collection<URI> locations );

    void removeActivePomLocations( URI... locations );

    void removeActivePomLocations( Collection<URI> locations );

    Set<URI> getActivePomLocations();

    int getActiveSourceCount();

    void addActiveSources( Collection<URI> sources );

    void addActiveSources( URI... sources );

    Set<URI> getActiveSources();

    void removeActiveSources( URI... sources );

    void removeActiveSources( Collection<URI> sources );

    String setProperty( String key, String value );

    String removeProperty( String key );

    String getProperty( String key );

    String getProperty( String key, String defaultVal );

    boolean registerView( GraphView view );

    void registerViewSelection( GraphView view, ProjectRef ref, ProjectVersionRef projectVersionRef );

    Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( GraphView view, Set<ProjectVersionRef> refs );

    ProjectVersionRef getPathTargetRef( GraphPath<?> path );

    List<ProjectVersionRef> getPathRefs( GraphView view, GraphPath<?> path );

}
