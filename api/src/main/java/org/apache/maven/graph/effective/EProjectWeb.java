/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public class EProjectWeb
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private transient Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> connectedProjects = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> variableSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final EGraphDriver driver;

    public EProjectWeb( final EGraphDriver driver )
    {
        this.driver = driver;
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

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getAllRelationships()
     */
    public Set<ProjectRelationship<?>> getAllRelationships()
        throws GraphDriverException
    {
        return new HashSet<ProjectRelationship<?>>( driver.getAllRelationships() );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isComplete()
     */
    public boolean isComplete()
    {
        return incompleteSubgraphs.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isConcrete()
     */
    public boolean isConcrete()
    {
        return variableSubgraphs.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getIncompleteSubgraphs()
     */
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( incompleteSubgraphs );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getVariableSubgraphs()
     */
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( variableSubgraphs );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#add(org.apache.maven.graph.effective.EProjectRelationships)
     */
    public void add( final EProjectRelationships rels )
    {
        if ( incompleteSubgraphs.contains( rels.getProjectRef() ) )
        {
            incompleteSubgraphs.remove( rels.getProjectRef() );
        }

        connectedProjects.add( rels.getProjectRef() );

        addAll( rels.getAllRelationships() );
    }

    public <T extends ProjectRelationship<?>> void add( final T rel )
    {
        if ( rel == null )
        {
            return;
        }

        incompleteSubgraphs.remove( rel.getDeclaring() );

        ProjectVersionRef target = rel.getTarget();
        if ( rel instanceof DependencyRelationship )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        driver.addRelationship( rel );

        if ( !target.getVersionSpec()
                    .isSingle() )
        {
            variableSubgraphs.add( target );
        }
        else if ( !connectedProjects.contains( target ) )
        {
            incompleteSubgraphs.add( target );
        }
    }

    public <T extends ProjectRelationship<?>> void addAll( final Collection<T> rels )
    {
        if ( rels == null )
        {
            return;
        }

        for ( final T rel : rels )
        {
            add( rel );
        }

        recomputeIncompleteSubgraphs();
    }

    public <T extends ProjectRelationship<?>> void addAll( final T... rels )
    {
        if ( rels == null )
        {
            return;
        }

        for ( final T rel : rels )
        {
            add( rel );
        }

        recomputeIncompleteSubgraphs();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#connect(org.apache.maven.graph.effective.EProjectWeb)
     */
    public void connect( final EProjectWeb otherWeb )
        throws GraphDriverException
    {
        final EGraphDriver otherDriver = otherWeb.getDriver();
        final Collection<ProjectVersionRef> otherNodes = otherDriver.getAllProjects();
        for ( final ProjectVersionRef node : otherNodes )
        {
            final Collection<? extends ProjectRelationship<?>> outEdges = otherDriver.getRelationshipsDeclaredBy( node );
            if ( incompleteSubgraphs.contains( node ) && outEdges != null && !outEdges.isEmpty() )
            {
                incompleteSubgraphs.remove( node );
            }
        }

        final Set<ProjectVersionRef> otherIncomplete = otherWeb.getIncompleteSubgraphs();
        for ( final ProjectVersionRef node : otherIncomplete )
        {
            if ( incompleteSubgraphs.contains( node ) )
            {
                continue;
            }

            if ( driver.containsProject( node ) )
            {
                final Collection<? extends ProjectRelationship<?>> outEdges = driver.getRelationshipsDeclaredBy( node );
                if ( outEdges == null || outEdges.isEmpty() )
                {
                    incompleteSubgraphs.add( node );
                }
            }
            else
            {
                incompleteSubgraphs.add( node );
            }
        }

        this.connectedProjects.addAll( otherWeb.connectedProjects );
        addAll( otherWeb.getAllRelationships() );
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

    private void readObject( final java.io.ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        incompleteSubgraphs = new HashSet<ProjectVersionRef>();
        connectedProjects = new HashSet<ProjectVersionRef>();
        variableSubgraphs = new HashSet<ProjectVersionRef>();
    }

    public void recomputeIncompleteSubgraphs()
    {
        for ( final ProjectVersionRef vertex : driver.getAllProjects() )
        {
            incompleteSubgraphs.remove( vertex );
        }
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
        throws GraphDriverException
    {
        return getAllRelationships();
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : cycles )
        {
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : cycles )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    public void addCycle( final EProjectCycle cycle )
    {
        this.cycles.add( cycle );
    }

    public Set<EProjectCycle> getCycles()
    {
        return new HashSet<EProjectCycle>( cycles );
    }

    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<? extends ProjectRelationship<?>> rels = driver.getRelationshipsTargeting( ref );
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    public EGraphDriver getDriver()
    {
        return driver;
    }
}
