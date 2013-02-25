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

    private final List<EProjectNet> superNets = new ArrayList<EProjectNet>();

    EProjectWeb( final EProjectNet parent, final EProjectKey... roots )
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final EProjectKey key : roots )
        {
            refs.add( key.getProject() );
        }

        this.driver = parent.getDriver()
                            .newInstanceFrom( this, refs.toArray( new ProjectVersionRef[] {} ) );

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

        return driver.addRelationship( rel );
    }

    public <T extends ProjectRelationship<?>> Set<T> addAll( final Collection<T> rels )
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
        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        for ( final ProjectVersionRef ref : driver.getAllProjects() )
        {
            final Collection<? extends ProjectRelationship<?>> inEdges = driver.getRelationshipsTargeting( ref );
            if ( inEdges == null || inEdges.isEmpty() )
            {
                result.add( ref );
            }
        }

        return result;
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
        if ( driver.containsProject( key.getProject() ) && !driver.isMissing( key.getProject() ) )
        {
            return new EProjectGraph( this, key );
        }

        return null;
    }

    public EProjectWeb getWeb( final EProjectKey... keys )
        throws GraphDriverException
    {
        for ( final EProjectKey key : keys )
        {
            if ( !driver.containsProject( key.getProject() ) || driver.isMissing( key.getProject() ) )
            {
                return null;
            }
        }

        return new EProjectWeb( this, keys );
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

}
