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

import static org.apache.maven.graph.effective.util.EGraphUtils.filterTerminalParents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.ref.EGraphFacts;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.apache.maven.graph.spi.effective.GloballyBackedGraphDriver;
import org.commonjava.util.logging.Logger;

public class EProjectGraph
    implements EProjectNet, KeyedProjectRelationshipCollection, Serializable
{

    private static final long serialVersionUID = 1L;

    private final Logger logger = new Logger( getClass() );

    private final EProjectKey key;

    private final EGraphDriver driver;

    private final List<EProjectNet> superNets = new ArrayList<EProjectNet>();

    public EProjectGraph( final EProjectNet parent, final EProjectKey key )
        throws GraphDriverException
    {
        this.key = key;
        this.driver = parent.getDriver()
                            .newInstanceFrom( this, null, key.getProject() );

        this.superNets.addAll( parent.getSuperNets() );
        this.superNets.add( parent );
    }

    public EProjectGraph( final EProjectNet parent, final ProjectRelationshipFilter filter, final EProjectKey key )
        throws GraphDriverException
    {
        this.key = key;
        this.driver = parent.getDriver()
                            .newInstanceFrom( this, filter, key.getProject() );

        this.superNets.addAll( parent.getSuperNets() );
        this.superNets.add( parent );
    }

    public EProjectGraph( final EProjectRelationships relationships, final ProjectRelationshipFilter filter,
                          final EGraphDriver driver )
        throws GraphDriverException
    {
        this.key = relationships.getKey();
        this.driver = driver.newInstanceFrom( null, filter, key.getProject() );

        add( relationships );
    }

    // TODO: If we construct like this based on contents of another graph, will we lose that graph's list of variable subgraphs??
    public EProjectGraph( final EProjectKey key, final Collection<ProjectRelationship<?>> relationships,
                          final Collection<EProjectRelationships> projectRelationships,
                          final Set<EProjectCycle> cycles, final ProjectRelationshipFilter filter,
                          final EGraphDriver driver )
        throws GraphDriverException
    {
        // NOTE: It does make sense to allow analysis of snapshots...it just requires different standards for mutability.
        //        final VersionSpec version = key.getProject()
        //                        .getVersionSpec();
        //
        //        if ( !version.isConcrete() )
        //        {
        //            throw new IllegalArgumentException(
        //                                                "Cannot build project graph rooted on non-concrete version of a project! Version is: "
        //                                                    + version );
        //        }

        this.key = key;
        this.driver = driver.newInstanceFrom( null, filter, key.getProject() );
        if ( cycles != null )
        {
            for ( final EProjectCycle cycle : cycles )
            {
                driver.addCycle( cycle );
            }
        }

        addAll( relationships );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }
    }

    public List<EProjectNet> getSuperNets()
    {
        return superNets;
    }

    public EProjectKey getKey()
    {
        return key;
    }

    public EGraphFacts getFacts()
    {
        return key.getFacts();
    }

    public Set<ProjectRelationship<?>> getFirstOrderRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactFirstOrderRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public Set<ProjectRelationship<?>> getExactFirstOrderRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( driver.getRelationshipsDeclaredBy( getRoot() ) );
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        final Collection<ProjectRelationship<?>> rels = driver.getAllRelationships();
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        logger.info( "Retrieving all relationships in graph: %s", key.getProject() );
        final Set<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public boolean isComplete()
    {
        return !driver.hasMissingProjects();
    }

    public boolean isConcrete()
    {
        return !driver.hasVariableProjects();
    }

    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getMissingProjects() );
    }

    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getVariableProjects() );
    }

    public static final class Builder
    {
        private final EProjectKey key;

        private final Set<ProjectRelationship<?>> relationships = new HashSet<ProjectRelationship<?>>();

        private final Set<EProjectRelationships> projects = new HashSet<EProjectRelationships>();

        private Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

        private final EGraphDriver driver;

        private ProjectRelationshipFilter filter;

        public Builder( final EProjectRelationships rels, final EGraphDriver driver )
        {
            this.driver = driver;
            this.key = rels.getKey();
            addFromDirectRelationships( rels );
        }

        public Builder( final ProjectVersionRef projectRef, final EGraphDriver driver, final String... activeProfiles )
        {
            this.driver = driver;
            this.key = new EProjectKey( projectRef, new EGraphFacts( activeProfiles ) );
        }

        public Builder( final EProjectKey key, final EGraphDriver driver )
        {
            this.key = key;
            this.driver = driver;
        }

        public Builder withFilter( final ProjectRelationshipFilter filter )
        {
            this.filter = filter;
            return this;
        }

        public Builder withParent( final ProjectVersionRef parent )
        {
            relationships.add( new ParentRelationship( key.getProject(), parent ) );
            return this;
        }

        public Builder withParent( final ProjectRelationship<ProjectVersionRef> parent )
        {
            if ( parent.getDeclaring()
                       .equals( key.getProject() ) )
            {
                relationships.add( parent );
            }
            else
            {
                relationships.add( parent.cloneFor( key.getProject() ) );
            }
            return this;
        }

        public Builder withDirectProjectRelationships( final EProjectRelationships... rels )
        {
            return withDirectProjectRelationships( Arrays.asList( rels ) );
        }

        public Builder withDirectProjectRelationships( final Collection<EProjectRelationships> rels )
        {
            for ( final EProjectRelationships relationships : rels )
            {
                if ( relationships.getKey()
                                  .equals( key ) )
                {
                    addFromDirectRelationships( relationships );
                }
                else
                {
                    this.projects.add( relationships );
                }
            }

            return this;
        }

        private void addFromDirectRelationships( final EProjectRelationships relationships )
        {
            this.relationships.clear();
            this.relationships.add( relationships.getParent() );
            this.relationships.addAll( relationships.getDependencies() );
            this.relationships.addAll( relationships.getManagedDependencies() );

            this.relationships.addAll( relationships.getPlugins() );
            this.relationships.addAll( relationships.getManagedPlugins() );

            this.relationships.addAll( relationships.getExtensions() );

            if ( relationships.getPluginDependencies() != null )
            {
                for ( final Map.Entry<PluginRelationship, List<PluginDependencyRelationship>> entry : relationships.getPluginDependencies()
                                                                                                                   .entrySet() )
                {
                    if ( entry.getValue() != null )
                    {
                        this.relationships.addAll( entry.getValue() );
                    }
                }
            }
        }

        public Builder withDependencies( final List<DependencyRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withDependencies( final DependencyRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withPlugins( final Collection<PluginRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withPlugins( final PluginRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withPluginLevelDependencies( final Collection<PluginDependencyRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withPluginLevelDependencies( final PluginDependencyRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withExtensions( final Collection<ExtensionRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withExtensions( final ExtensionRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withExactRelationships( final Collection<ProjectRelationship<?>> relationships )
        {
            this.relationships.addAll( relationships );
            return this;
        }

        public Builder withExactRelationships( final ProjectRelationship<?>... relationships )
        {
            this.relationships.addAll( Arrays.asList( relationships ) );
            return this;
        }

        public Builder withRelationships( final Collection<ProjectRelationship<?>> relationships )
        {
            final Set<PluginDependencyRelationship> pluginDepRels = new HashSet<PluginDependencyRelationship>();
            for ( final ProjectRelationship<?> rel : relationships )
            {
                switch ( rel.getType() )
                {
                    case DEPENDENCY:
                    {
                        final DependencyRelationship dr = (DependencyRelationship) rel;
                        withDependencies( dr );

                        break;
                    }
                    case PLUGIN:
                    {
                        final PluginRelationship pr = (PluginRelationship) rel;
                        withPlugins( pr );

                        break;
                    }
                    case EXTENSION:
                    {
                        withExtensions( (ExtensionRelationship) rel );
                        break;
                    }
                    case PLUGIN_DEP:
                    {
                        // load all plugin relationships first.
                        pluginDepRels.add( (PluginDependencyRelationship) rel );
                        break;
                    }
                    case PARENT:
                    {
                        withParent( (ParentRelationship) rel );
                        break;
                    }
                }
            }

            withPluginLevelDependencies( pluginDepRels );

            return this;
        }

        public EProjectGraph build()
            throws GraphDriverException
        {
            boolean foundParent = false;
            for ( final ProjectRelationship<?> rel : relationships )
            {
                if ( rel instanceof ParentRelationship && rel.getDeclaring()
                                                             .equals( key.getProject() ) )
                {
                    foundParent = true;
                    break;
                }
            }

            if ( !foundParent )
            {
                relationships.add( new ParentRelationship( key.getProject(), key.getProject() ) );
            }

            return new EProjectGraph( key, relationships, projects, cycles, filter, driver );
        }

        public Builder withCycles( final Set<EProjectCycle> cycles )
        {
            if ( cycles != null )
            {
                this.cycles = cycles;
            }

            return this;
        }

    }

    public Set<ProjectRelationship<?>> add( final EProjectRelationships rels )
    {
        return addAll( rels.getExactAllRelationships() );
    }

    //    private <T extends ProjectRelationship<?>> boolean add( final T rel )
    //    {
    //        if ( rel == null )
    //        {
    //            return false;
    //        }
    //
    //        ProjectVersionRef target = rel.getTarget();
    //        if ( rel instanceof DependencyRelationship )
    //        {
    //            target = ( (ArtifactRef) target ).asProjectVersionRef();
    //        }
    //
    //        return driver.addRelationships( rel )
    //                     .isEmpty();
    //    }

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

    public ProjectVersionRef getRoot()
    {
        return key.getProject();
    }

    public void traverse( final ProjectNetTraversal traversal )
        throws GraphDriverException
    {
        traverse( getRoot(), traversal );
    }

    protected void traverse( final ProjectVersionRef ref, final ProjectNetTraversal traversal )
        throws GraphDriverException
    {
        driver.traverse( traversal, this, ref );
    }

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
    public EProjectGraph filteredInstance( final ProjectRelationshipFilter filter )
        throws GraphDriverException
    {
        return new EProjectGraph( this, filter, key );
    }

    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        driver.addDisconnectedProject( ref );
    }
}
