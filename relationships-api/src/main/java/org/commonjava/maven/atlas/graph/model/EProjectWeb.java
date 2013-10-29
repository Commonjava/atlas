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
package org.commonjava.maven.atlas.graph.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class EProjectWeb
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private final GraphView view;

    public EProjectWeb( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef... refs )
    {
        this.view = new GraphView( workspace, filter, refs );
    }

    public EProjectWeb( final GraphWorkspace session )
    {
        this.view = new GraphView( session );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getAllRelationships()
     */
    @Override
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( view.getDatabase()
                                                        .getAllRelationships( view ) );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isComplete()
     */
    @Override
    public boolean isComplete()
    {
        return !view.getDatabase()
                    .hasMissingProjects( view );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isConcrete()
     */
    @Override
    public boolean isConcrete()
    {
        return !view.getDatabase()
                    .hasVariableProjects( view );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getIncompleteSubgraphs()
     */
    @Override
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( view.getDatabase()
                                                .getMissingProjects( view ) );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getVariableSubgraphs()
     */
    @Override
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( view.getDatabase()
                                                .getVariableProjects( view ) );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#add(org.apache.maven.graph.effective.EProjectRelationships)
     */
    public Set<ProjectRelationship<?>> add( final EProjectDirectRelationships rels )
        throws GraphDriverException
    {
        return addAll( rels.getAllRelationships() );
    }

    public <T extends ProjectRelationship<?>> boolean add( final T rel )
    {
        if ( rel == null )
        {
            return false;
        }

        return view.getDatabase()
                   .addRelationships( rel )
                   .isEmpty();
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

        return result;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends ProjectRelationship<?>> Set<T> addAll( final T... rels )
        throws GraphDriverException
    {
        if ( rels == null )
        {
            return null;
        }

        final Set<T> result = new HashSet<T>();
        for ( final T rel : rels )
        {
            if ( add( rel ) )
            {
                result.add( rel );
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#connect(org.apache.maven.graph.effective.EProjectWeb)
     */
    public void connect( final EProjectNet otherWeb )
        throws GraphDriverException
    {
        if ( !otherWeb.getDatabase()
                      .equals( getDatabase() ) )
        {
            addAll( otherWeb.getExactAllRelationships() );
        }
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#traverse(org.apache.maven.graph.common.ref.ProjectVersionRef, org.apache.maven.graph.effective.traverse.ProjectGraphTraversal)
     */
    public void traverse( final ProjectVersionRef start, final ProjectNetTraversal traversal )
        throws GraphDriverException
    {
        view.getDatabase()
            .traverse( view, traversal, start );
    }

    public Set<ProjectRelationship<?>> getUserRelationships( final ProjectVersionRef ref )
    {
        if ( !view.getDatabase()
                  .containsProject( view, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( view.getDatabase()
                                                        .getRelationshipsTargeting( view, ref ) );
    }

    public Set<ProjectRelationship<?>> getDirectRelationships( final ProjectVersionRef ref )
    {
        if ( !view.getDatabase()
                  .containsProject( view, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( view.getDatabase()
                                                        .getRelationshipsDeclaredBy( view, ref ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return view.getRoots();
    }

    @Override
    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        return getAllRelationships();
    }

    @Override
    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        return view.getDatabase()
                   .isCycleParticipant( view, ref );
    }

    @Override
    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        return view.getDatabase()
                   .isCycleParticipant( view, rel );
    }

    @Override
    public void addCycle( final EProjectCycle cycle )
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
                                                                      .getRelationshipsTargeting( view, ref.asProjectVersionRef() );
        if ( rels == null )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    @Override
    public GraphDatabaseDriver getDatabase()
    {
        return view.getDatabase();
    }

    //    @Override
    //    public EProjectGraph getGraph( final ProjectVersionRef ref, final EGraphSession session )
    //        throws GraphDriverException
    //    {
    //        return getGraph( null, ref, session );
    //    }
    //
    //    @Override
    //    public EProjectGraph getGraph( final ProjectRelationshipFilter filter, final ProjectVersionRef ref,
    //                                   final EGraphSession session )
    //        throws GraphDriverException
    //    {
    //        if ( view.getDatabase().containsProject( view, ref ) && !view.getDatabase().isMissing( view, ref ) )
    //        {
    //            return new EProjectGraph( session, this, filter, ref );
    //        }
    //
    //        return null;
    //    }
    //
    //    @Override
    //    public EProjectWeb getWeb( final EGraphSession session, final ProjectVersionRef... refs )
    //        throws GraphDriverException
    //    {
    //        return getWeb( session, null, refs );
    //    }
    //
    //    @Override
    //    public EProjectWeb getWeb( final EGraphSession session, final ProjectRelationshipFilter filter,
    //                               final ProjectVersionRef... refs )
    //        throws GraphDriverException
    //    {
    //        for ( final ProjectVersionRef ref : refs )
    //        {
    //            if ( !view.getDatabase().containsProject( ref ) || view.getDatabase().isMissing( ref ) )
    //            {
    //                return null;
    //            }
    //        }
    //
    //        return new EProjectWeb( session, this, filter, refs );
    //    }

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
    {
        view.getDatabase()
            .addMetadata( key.getProject(), name, value );
    }

    @Override
    public void addMetadata( final EProjectKey key, final Map<String, String> metadata )
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

    @Override
    public Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef... projectVersionRefs )
    {
        return view.getDatabase()
                   .getAllPathsTo( view, projectVersionRefs );
    }

    @Override
    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return view.getDatabase()
                   .introducesCycle( view, rel );
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
    @SuppressWarnings( "unchecked" )
    public EProjectNet filteredInstance( final ProjectRelationshipFilter filter )
        throws GraphDriverException
    {
        return new EProjectWeb( view.getWorkspace(), filter, getRoots().toArray( new ProjectVersionRef[] {} ) );
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        view.getDatabase()
            .addDisconnectedProject( ref );
    }

    @Override
    public String toString()
    {
        return String.format( "EProjectWeb [roots: %s, session=%s]", view.getRoots(), view.getWorkspace() );
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
        final Set<URI> sources = new HashSet<>();
        for ( final ProjectRelationship<?> rel : rels )
        {
            sources.addAll( rel.getSources() );
        }

        return sources;
    }
}
