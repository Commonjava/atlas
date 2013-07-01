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
package org.apache.maven.graph.effective;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public class EProjectWeb
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private final EGraphDriver driver;

    private final GraphView view;

    EProjectWeb( final GraphWorkspace session, final EGraphDriver driver, final ProjectRelationshipFilter filter,
                 final ProjectVersionRef... refs )
    {
        this.view = new GraphView( session, filter, refs );
        this.driver = driver;
    }

    public EProjectWeb( final GraphWorkspace session, final EGraphDriver driver )
    {
        this.view = new GraphView( session );
        this.driver = driver;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getAllRelationships()
     */
    @Override
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( driver.getAllRelationships( view ) );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isComplete()
     */
    @Override
    public boolean isComplete()
    {
        return !driver.hasMissingProjects( view );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isConcrete()
     */
    @Override
    public boolean isConcrete()
    {
        return !driver.hasVariableProjects( view );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getIncompleteSubgraphs()
     */
    @Override
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getMissingProjects( view ) );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getVariableSubgraphs()
     */
    @Override
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getVariableProjects( view ) );
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

        return driver.addRelationships( rel )
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

        final Set<ProjectRelationship<?>> rejected =
            driver.addRelationships( rels.toArray( new ProjectRelationship<?>[] {} ) );
        result.removeAll( rejected );

        if ( !result.isEmpty() )
        {
            driver.recomputeIncompleteSubgraphs();
        }

        return result;
    }

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

        driver.recomputeIncompleteSubgraphs();

        return result;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#connect(org.apache.maven.graph.effective.EProjectWeb)
     */
    public void connect( final EProjectWeb otherWeb )
        throws GraphDriverException
    {
        if ( !otherWeb.getDriver()
                      .equals( getDriver() ) )
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
        driver.traverse( view, traversal, this, start );
    }

    public Set<ProjectRelationship<?>> getUserRelationships( final ProjectVersionRef ref )
    {
        if ( !driver.containsProject( view, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( driver.getRelationshipsTargeting( view, ref ) );
    }

    public Set<ProjectRelationship<?>> getDirectRelationships( final ProjectVersionRef ref )
    {
        if ( !driver.containsProject( view, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( driver.getRelationshipsDeclaredBy( view, ref ) );
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
        return driver.isCycleParticipant( view, ref );
    }

    @Override
    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        return driver.isCycleParticipant( view, rel );
    }

    @Override
    public void addCycle( final EProjectCycle cycle )
    {
        driver.addCycle( cycle );
    }

    @Override
    public Set<EProjectCycle> getCycles()
    {
        return driver.getCycles( view );
    }

    @Override
    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<? extends ProjectRelationship<?>> rels =
            driver.getRelationshipsTargeting( view, ref.asProjectVersionRef() );
        if ( rels == null )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    @Override
    public EGraphDriver getDriver()
    {
        return driver;
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
    //        if ( driver.containsProject( view, ref ) && !driver.isMissing( view, ref ) )
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
    //            if ( !driver.containsProject( ref ) || driver.isMissing( ref ) )
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
        return driver.containsProject( view, ref ) && !driver.isMissing( view, ref );
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects()
    {
        return driver.getAllProjects( view );
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        return driver.getMetadata( ref );
    }

    @Override
    public void addMetadata( final EProjectKey key, final String name, final String value )
    {
        driver.addMetadata( key.getProject(), name, value );
    }

    @Override
    public void addMetadata( final EProjectKey key, final Map<String, String> metadata )
    {
        driver.addMetadata( key.getProject(), metadata );
    }

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return driver.getProjectsWithMetadata( view, key );
    }

    @Override
    public void reindex()
        throws GraphDriverException
    {
        driver.reindex();
    }

    @Override
    public Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef... projectVersionRefs )
    {
        return driver.getAllPathsTo( view, projectVersionRefs );
    }

    @Override
    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return driver.introducesCycle( view, rel );
    }

    @Override
    public GraphWorkspace getSession()
    {
        return view.getWorkspace();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public EProjectWeb filteredInstance( final ProjectRelationshipFilter filter )
        throws GraphDriverException
    {
        return new EProjectWeb( view.getWorkspace(), driver, filter, getRoots().toArray( new ProjectVersionRef[] {} ) );
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        driver.addDisconnectedProject( ref );
    }

    @Override
    public String toString()
    {
        return String.format( "EProjectWeb [roots: %s, session=%s]", view.getRoots(), view.getWorkspace() );
    }

    @Override
    public boolean isMissing( final ProjectVersionRef ref )
    {
        return driver.isMissing( view, ref );
    }
}
