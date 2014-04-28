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
package org.commonjava.maven.atlas.graph.model;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface EProjectNet
    extends EProjectRelationshipCollection
{

    <T extends ProjectRelationship<?>> Set<T> addAll( Collection<T> rels )
        throws RelationshipGraphConnectionException;

    void addCycle( final EProjectCycle cycle )
        throws RelationshipGraphConnectionException;

    void addDisconnectedProject( ProjectVersionRef ref )
        throws RelationshipGraphConnectionException;

    void addMetadata( EProjectKey key, String name, String value )
        throws RelationshipGraphConnectionException;

    void addMetadata( EProjectKey key, Map<String, String> metadata )
        throws RelationshipGraphConnectionException;

    //    Map<ProjectVersionRef, SingleVersion> clearSelectedVersions()
    //        throws GraphDriverException;

    boolean containsGraph( ProjectVersionRef ref );

    <T extends EProjectNet> T filteredInstance( ProjectRelationshipFilter filter )
        throws RelationshipGraphConnectionException;

    boolean introducesCycle( ProjectRelationship<?> rel );

    boolean isComplete();

    boolean isConcrete();

    //    void recomputeIncompleteSubgraphs();

    boolean isCycleParticipant( ProjectRelationship<?> rel );

    boolean isCycleParticipant( ProjectVersionRef ref );

    Set<EProjectCycle> getCycles();

    RelationshipGraphConnection getDatabase();

    GraphWorkspace getWorkspace();

    GraphView getView();

    Set<ProjectVersionRef> getIncompleteSubgraphs();

    Set<ProjectVersionRef> getVariableSubgraphs();

    Set<ProjectRelationship<?>> getRelationshipsTargeting( ProjectVersionRef ref );

    Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef... refs );

    //    EProjectGraph getGraph( ProjectRelationshipFilter filter, ProjectVersionRef ref, EGraphSession session )
    //        throws GraphDriverException;
    //
    //    EProjectGraph getGraph( ProjectVersionRef ref, EGraphSession session )
    //        throws GraphDriverException;
    //
    //    EProjectWeb getWeb( EGraphSession session, ProjectVersionRef... refs )
    //        throws GraphDriverException;
    //
    //    EProjectWeb getWeb( EGraphSession session, ProjectRelationshipFilter filter, ProjectVersionRef... refs )
    //        throws GraphDriverException;

    Set<ProjectVersionRef> getAllProjects();

    //    Set<ProjectRelationship<?>> getAllRelationships();

    Map<String, String> getMetadata( ProjectVersionRef ref );

    Set<ProjectVersionRef> getProjectsWithMetadata( String key );

    void reindex()
        throws RelationshipGraphConnectionException;

    boolean isMissing( ProjectVersionRef ref );

    public abstract Set<URI> getSources();

    //    ProjectVersionRef selectVersionFor( ProjectVersionRef variable, SingleVersion version )
    //        throws GraphDriverException;

}
