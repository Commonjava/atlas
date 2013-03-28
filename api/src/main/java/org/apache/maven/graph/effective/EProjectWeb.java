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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.apache.maven.graph.spi.effective.GloballyBackedGraphDriver;

public class EProjectWeb
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private final EGraphDriver driver;

    private final List<EProjectNet> superNets = new ArrayList<EProjectNet>();

    EProjectWeb( final EProjectNet parent, final ProjectRelationshipFilter filter, final EProjectKey... roots )
        throws GraphDriverException
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final EProjectKey key : roots )
        {
            refs.add( key.getProject() );
        }

        this.driver = parent.getDriver()
                            .newInstanceFrom( this, filter, refs.toArray( new ProjectVersionRef[] {} ) );

        this.superNets.addAll( parent.getSuperNets() );
        this.superNets.add( parent );
    }

    public EProjectWeb( final EGraphDriver driver )
    {
        this.driver = driver;
    }

    public EProjectWeb( final Set<ProjectRelationship<?>> relationships,
                        final Collection<EProjectRelationships> projectRelationships, final Set<EProjectCycle> cycles,
                        final EGraphDriver driver )
    {
        this.driver = driver;
        addAll( relationships );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }

        if ( cycles != null )
        {
            for ( final EProjectCycle cycle : cycles )
            {
                addCycle( cycle );
            }
        }
    }

    public EProjectWeb( final Collection<ProjectRelationship<?>> relationships,
                        final Collection<EProjectRelationships> projectRelationships, final EGraphDriver driver )
    {
        this.driver = driver;
        addAll( relationships );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }
    }

    public EProjectWeb( final Set<ProjectRelationship<?>> relationships, final EGraphDriver driver )
    {
        this.driver = driver;
        addAll( relationships );
    }

    public EProjectWeb( final EProjectWeb parent, final ProjectRelationshipFilter filter,
                        final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        this.driver = parent.getDriver()
                            .newInstanceFrom( this, filter, roots );

        this.superNets.addAll( parent.getSuperNets() );
        this.superNets.add( parent );
    }

    public List<EProjectNet> getSuperNets()
    {
        return superNets;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getAllRelationships()
     */
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( driver.getAllRelationships() );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isComplete()
     */
    public boolean isComplete()
    {
        return !driver.hasMissingProjects();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isConcrete()
     */
    public boolean isConcrete()
    {
        return !driver.hasVariableProjects();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getIncompleteSubgraphs()
     */
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getMissingProjects() );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getVariableSubgraphs()
     */
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getVariableProjects() );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#add(org.apache.maven.graph.effective.EProjectRelationships)
     */
    public Set<ProjectRelationship<?>> add( final EProjectRelationships rels )
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

    public <T extends ProjectRelationship<?>> Set<T> addAll( final Collection<T> rels )
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
        if ( !otherWeb.isDerivedFrom( this ) )
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
        driver.traverse( traversal, this, start );
    }

    public Set<ProjectRelationship<?>> getUserRelationships( final ProjectVersionRef ref )
    {
        if ( !driver.containsProject( ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( driver.getRelationshipsTargeting( ref ) );
    }

    public Set<ProjectRelationship<?>> getDirectRelationships( final ProjectVersionRef ref )
    {
        if ( !driver.containsProject( ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( driver.getRelationshipsDeclaredBy( ref ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return driver.getRoots();
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        return getAllRelationships();
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        return driver.isCycleParticipant( ref );
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        return driver.isCycleParticipant( rel );
    }

    public void addCycle( final EProjectCycle cycle )
    {
        driver.addCycle( cycle );
    }

    public Set<EProjectCycle> getCycles()
    {
        return driver.getCycles();
    }

    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<? extends ProjectRelationship<?>> rels =
            driver.getRelationshipsTargeting( ref.asProjectVersionRef() );
        if ( rels == null )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    public EGraphDriver getDriver()
    {
        return driver;
    }

    public boolean isDerivedFrom( final EProjectNet net )
    {
        return driver.isDerivedFrom( net.getDriver() );
    }

    public EProjectGraph getGraph( final EProjectKey key )
        throws GraphDriverException
    {
        return getGraph( null, key );
    }

    public EProjectGraph getGraph( final ProjectRelationshipFilter filter, final EProjectKey key )
        throws GraphDriverException
    {
        if ( driver.containsProject( key.getProject() ) && !driver.isMissing( key.getProject() ) )
        {
            return new EProjectGraph( this, filter, key );
        }

        return null;
    }

    public EProjectWeb getWeb( final EProjectKey... keys )
        throws GraphDriverException
    {
        return getWeb( null, keys );
    }

    public EProjectWeb getWeb( final ProjectRelationshipFilter filter, final EProjectKey... keys )
        throws GraphDriverException
    {
        for ( final EProjectKey key : keys )
        {
            if ( !driver.containsProject( key.getProject() ) || driver.isMissing( key.getProject() ) )
            {
                return null;
            }
        }

        return new EProjectWeb( this, filter, keys );
    }

    public boolean containsGraph( final EProjectKey key )
    {
        return driver.containsProject( key.getProject() ) && !driver.isMissing( key.getProject() );
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return driver.getAllProjects();
    }

    public Map<String, String> getMetadata( final EProjectKey key )
    {
        return driver.getProjectMetadata( key.getProject() );
    }

    public void addMetadata( final EProjectKey key, final String name, final String value )
    {
        driver.addProjectMetadata( key.getProject(), name, value );
    }

    public void addMetadata( final EProjectKey key, final Map<String, String> metadata )
    {
        driver.addProjectMetadata( key.getProject(), metadata );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return driver.getProjectsWithMetadata( key );
    }

    public void reindex()
        throws GraphDriverException
    {
        driver.reindex();
    }

    public boolean connectFor( final EProjectKey key )
        throws GraphDriverException
    {
        final EGraphDriver driver = getDriver();
        if ( driver instanceof GloballyBackedGraphDriver )
        {
            if ( ( (GloballyBackedGraphDriver) driver ).includeGraph( key.getProject() ) )
            {
                for ( final EProjectNet net : getSuperNets() )
                {
                    final EGraphDriver d = net.getDriver();
                    if ( d instanceof GloballyBackedGraphDriver )
                    {
                        ( (GloballyBackedGraphDriver) d ).includeGraph( key.getProject() );
                    }
                }

                return true;
            }
        }

        return false;
    }

    public void connect( final EProjectGraph graph )
        throws GraphDriverException
    {
        if ( getDriver() instanceof GloballyBackedGraphDriver )
        {
            connectFor( graph.getKey() );
        }
        else if ( !graph.isDerivedFrom( this ) )
        {
            addAll( graph.getExactAllRelationships() );
            for ( final EProjectNet net : getSuperNets() )
            {
                net.addAll( graph.getExactAllRelationships() );
            }
        }
    }

    public ProjectVersionRef selectVersionFor( final ProjectVersionRef variable, final SingleVersion version )
        throws GraphDriverException
    {
        final ProjectVersionRef ref = variable.selectVersion( version );
        driver.selectVersionFor( variable, ref );

        return ref;
    }

    public Map<ProjectVersionRef, ProjectVersionRef> clearSelectedVersions()
        throws GraphDriverException
    {
        return driver.clearSelectedVersions();
    }

    public Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef ref )
    {
        return driver.getAllPathsTo( ref );
    }

    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return driver.introducesCycle( rel );
    }

    @SuppressWarnings( "unchecked" )
    public EProjectWeb filteredInstance( final ProjectRelationshipFilter filter )
        throws GraphDriverException
    {
        return new EProjectWeb( this, filter, getRoots().toArray( new ProjectVersionRef[] {} ) );
    }

    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        driver.addDisconnectedProject( ref );
    }
}
