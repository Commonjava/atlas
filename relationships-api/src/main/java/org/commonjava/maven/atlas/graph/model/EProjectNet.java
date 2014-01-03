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
package org.commonjava.maven.atlas.graph.model;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface EProjectNet
    extends EProjectRelationshipCollection
{

    <T extends ProjectRelationship<?>> Set<T> addAll( Collection<T> rels )
        throws GraphDriverException;

    void addCycle( final EProjectCycle cycle );

    void addDisconnectedProject( ProjectVersionRef ref );

    void addMetadata( EProjectKey key, String name, String value );

    void addMetadata( EProjectKey key, Map<String, String> metadata );

    //    Map<ProjectVersionRef, SingleVersion> clearSelectedVersions()
    //        throws GraphDriverException;

    boolean containsGraph( ProjectVersionRef ref );

    <T extends EProjectNet> T filteredInstance( ProjectRelationshipFilter filter )
        throws GraphDriverException;

    boolean introducesCycle( ProjectRelationship<?> rel );

    boolean isComplete();

    boolean isConcrete();

    //    void recomputeIncompleteSubgraphs();

    boolean isCycleParticipant( ProjectRelationship<?> rel );

    boolean isCycleParticipant( ProjectVersionRef ref );

    Set<EProjectCycle> getCycles();

    GraphDatabaseDriver getDatabase();

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
        throws GraphDriverException;

    boolean isMissing( ProjectVersionRef ref );

    public abstract Set<URI> getSources();

    //    ProjectVersionRef selectVersionFor( ProjectVersionRef variable, SingleVersion version )
    //        throws GraphDriverException;

}
