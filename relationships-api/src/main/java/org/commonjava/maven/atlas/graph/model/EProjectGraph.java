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

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.filterTerminalParents;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class EProjectGraph
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private final ProjectVersionRef project;

    private final GraphView view;

    public EProjectGraph( final GraphWorkspace session, final ProjectVersionRef ref )
    {
        this.view = new GraphView( session, ref );
        this.project = ref;
    }

    public EProjectGraph( final GraphWorkspace session, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                          final ProjectVersionRef ref )
    {
        this.view = new GraphView( session, filter, mutator, ref );
        this.project = ref;
    }

    public EProjectGraph( final GraphView view )
    {
        this.view = view;
        this.project = view.getRoots()
                           .iterator()
                           .next();
    }

    public Set<ProjectRelationship<?>> getFirstOrderRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactFirstOrderRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public Set<ProjectRelationship<?>> getExactFirstOrderRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( view.getDatabase()
                                                        .getRelationshipsDeclaredBy( view, getRoot() ) );
    }

    @Override
    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        final Collection<ProjectRelationship<?>> rels = view.getDatabase()
                                                            .getAllRelationships( view );
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    @Override
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    @Override
    public boolean isComplete()
    {
        return !view.getDatabase()
                    .hasMissingProjects( view );
    }

    @Override
    public boolean isConcrete()
    {
        return !view.getDatabase()
                    .hasVariableProjects( view );
    }

    @Override
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( view.getDatabase()
                                                .getMissingProjects( view ) );
    }

    @Override
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( view.getDatabase()
                                                .getVariableProjects( view ) );
    }

    public Set<ProjectRelationship<?>> add( final EProjectDirectRelationships rels )
        throws GraphDriverException
    {
        return addAll( rels.getExactAllRelationships() );
    }

    @Override
    public <T extends ProjectRelationship<?>> Set<T> addAll( final Collection<T> rels )
        throws GraphDriverException
    {
        if ( rels == null )
        {
            return null;
        }

        final Set<T> result = new HashSet<T>( rels );

        final Set<ProjectRelationship<?>> rejected = view.getDatabase()
                                                         .addRelationships( rels.toArray( new ProjectRelationship<?>[] {} ) );
        result.removeAll( rejected );

        if ( !result.isEmpty() )
        {
            view.getDatabase()
                .recomputeIncompleteSubgraphs();
        }

        return result;
    }

    public ProjectVersionRef getRoot()
    {
        return project;
    }

    public void traverse( final ProjectNetTraversal traversal )
        throws GraphDriverException
    {
        traverse( getRoot(), traversal );
    }

    protected void traverse( final ProjectVersionRef ref, final ProjectNetTraversal traversal )
        throws GraphDriverException
    {
        view.getDatabase()
            .traverse( view, traversal, this, ref );
    }

    @Override
    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : getCycles() )
        {
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : getCycles() )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void addCycle( final EProjectCycle cycle )
        throws GraphDriverException
    {
        view.getDatabase()
            .addCycle( cycle );
    }

    @Override
    public Set<EProjectCycle> getCycles()
    {
        return view.getDatabase()
                   .getCycles( view );
    }

    @Override
    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<? extends ProjectRelationship<?>> rels = view.getDatabase()
                                                                      .getRelationshipsTargeting( view, ref );
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    @Override
    public GraphDatabaseDriver getDatabase()
    {
        return view.getDatabase();
    }

    @Override
    public GraphWorkspace getWorkspace()
    {
        return view.getWorkspace();
    }

    @Override
    public GraphView getView()
    {
        return view;
    }

    @Override
    public boolean containsGraph( final ProjectVersionRef ref )
    {
        return view.getDatabase()
                   .containsProject( view, ref );
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects()
    {
        return view.getDatabase()
                   .getAllProjects( view );
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        return view.getDatabase()
                   .getMetadata( ref );
    }

    @Override
    public void addMetadata( final EProjectKey key, final String name, final String value )
        throws GraphDriverException
    {
        view.getDatabase()
            .addMetadata( key.getProject(), name, value );
    }

    @Override
    public void addMetadata( final EProjectKey key, final Map<String, String> metadata )
        throws GraphDriverException
    {
        view.getDatabase()
            .setMetadata( key.getProject(), metadata );
    }

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return view.getDatabase()
                   .getProjectsWithMetadata( view, key );
    }

    @Override
    public void reindex()
        throws GraphDriverException
    {
        view.getDatabase()
            .reindex();
    }

    //    @Override
    //    public ProjectVersionRef selectVersionFor( final ProjectVersionRef variable, final SingleVersion version )
    //        throws GraphDriverException
    //    {
    //        return session.selectVersion( variable, version );
    //    }
    //
    //    @Override
    //    public Map<ProjectVersionRef, SingleVersion> clearSelectedVersions()
    //        throws GraphDriverException
    //    {
    //        return session.clearVersionSelections();
    //    }

    @Override
    public Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef... refs )
    {
        return view.getDatabase()
                   .getAllPathsTo( view, refs );
    }

    @Override
    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return view.getDatabase()
                   .introducesCycle( view, rel );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public EProjectGraph filteredInstance( final ProjectRelationshipFilter filter )
        throws GraphDriverException
    {
        return new EProjectGraph( view.getWorkspace(), filter, view.getMutator(), project );
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
        throws GraphDriverException
    {
        view.getDatabase()
            .addDisconnectedProject( ref );
    }

    @Override
    public String toString()
    {
        return String.format( "EProjectGraph [root: %s, session: %s]", project, view.getWorkspace() );
    }

    @Override
    public boolean isMissing( final ProjectVersionRef ref )
    {
        return view.getDatabase()
                   .isMissing( view, ref );
    }

    @Override
    public Set<URI> getSources()
    {
        final Set<ProjectRelationship<?>> rels = getAllRelationships();
        final Set<URI> sources = new HashSet<URI>();
        for ( final ProjectRelationship<?> rel : rels )
        {
            sources.addAll( rel.getSources() );
        }

        return sources;
    }

}
