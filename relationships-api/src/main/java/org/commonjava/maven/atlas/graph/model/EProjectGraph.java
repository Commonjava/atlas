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

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.filterTerminalParents;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.EGraphDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

public class EProjectGraph
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private final Logger logger = new Logger( getClass() );

    private final ProjectVersionRef project;

    private final EGraphDriver driver;

    private final GraphView view;

    public EProjectGraph( final GraphWorkspace session, final EGraphDriver driver, final ProjectVersionRef ref )
    {
        this.view = new GraphView( session, ref );
        this.project = ref;
        this.driver = driver;
    }

    public EProjectGraph( final GraphWorkspace session, final EGraphDriver driver,
                          final ProjectRelationshipFilter filter, final ProjectVersionRef ref )
    {
        this.view = new GraphView( session, filter, ref );
        this.project = ref;
        this.driver = driver;
    }

    //    public EProjectGraph( final EGraphSession session, final EProjectRelationships relationships,
    //                          final ProjectRelationshipFilter filter, final EGraphDriver driver )
    //        throws GraphDriverException
    //    {
    //        this.session = session;
    //        final EProjectKey key = relationships.getKey();
    //
    //        this.project = key.getProject();
    //        this.sources = Collections.singleton( key.getSource() );
    //        this.driver = driver.newInstance( session, null, filter, key.getProject() );
    //
    //        add( relationships );
    //    }
    //
    //    // TODO: If we construct like this based on contents of another graph, will we lose that graph's list of variable subgraphs??
    //    public EProjectGraph( final EGraphSession session, final EProjectKey key,
    //                          final Collection<ProjectRelationship<?>> relationships,
    //                          final Collection<EProjectRelationships> projectRelationships,
    //                          final Set<EProjectCycle> cycles, final ProjectRelationshipFilter filter,
    //                          final EGraphDriver driver )
    //        throws GraphDriverException
    //    {
    //        // NOTE: It does make sense to allow analysis of snapshots...it just requires different standards for mutability.
    //        //        final VersionSpec version = key.getProject()
    //        //                        .getVersionSpec();
    //        //
    //        //        if ( !version.isConcrete() )
    //        //        {
    //        //            throw new IllegalArgumentException(
    //        //                                                "Cannot build project graph rooted on non-concrete version of a project! Version is: "
    //        //                                                    + version );
    //        //        }
    //
    //        this.session = session;
    //        this.project = key.getProject();
    //        this.sources = Collections.singleton( key.getSource() );
    //        this.driver = driver.newInstance( session, this, filter, key.getProject() );
    //        if ( cycles != null )
    //        {
    //            for ( final EProjectCycle cycle : cycles )
    //            {
    //                driver.addCycle( cycle );
    //            }
    //        }
    //
    //        addAll( relationships );
    //        for ( final EProjectRelationships project : projectRelationships )
    //        {
    //            add( project );
    //        }
    //    }

    public Set<ProjectRelationship<?>> getFirstOrderRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactFirstOrderRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public Set<ProjectRelationship<?>> getExactFirstOrderRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( driver.getRelationshipsDeclaredBy( view, getRoot() ) );
    }

    @Override
    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        final Collection<ProjectRelationship<?>> rels = driver.getAllRelationships( view );
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    @Override
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        logger.info( "Retrieving all relationships in graph: %s", project );
        final Set<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    @Override
    public boolean isComplete()
    {
        return !driver.hasMissingProjects( view );
    }

    @Override
    public boolean isConcrete()
    {
        return !driver.hasVariableProjects( view );
    }

    @Override
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getMissingProjects( view ) );
    }

    @Override
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( driver.getVariableProjects( view ) );
    }

    //    public static final class Builder
    //    {
    //        private final ProjectVersionRef root;
    //
    //        private final Set<URI> sources = new HashSet<URI>();
    //
    //        private final Set<ProjectRelationship<?>> relationships = new HashSet<ProjectRelationship<?>>();
    //
    //        private final Set<EProjectRelationships> projects = new HashSet<EProjectRelationships>();
    //
    //        private Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
    //
    //        private final EGraphDriver driver;
    //
    //        private ProjectRelationshipFilter filter;
    //
    //        private final EGraphSession session;
    //
    //        private final URI rootSource;
    //
    //        public Builder( final EGraphSessionConfiguration config, final EProjectRelationships rels,
    //                        final EGraphDriver driver )
    //            throws GraphDriverException
    //        {
    //            this.session = driver.createSession( config );
    //            this.driver = driver;
    //            this.root = rels.getKey()
    //                            .getProject();
    //            this.rootSource = rels.getKey()
    //                                  .getSource();
    //            this.sources.add( rels.getKey()
    //                                  .getSource() );
    //            addFromDirectRelationships( rels );
    //        }
    //
    //        public Builder( final EGraphSessionConfiguration config, final URI source, final ProjectVersionRef projectRef,
    //                        final EGraphDriver driver, final String... activeProfiles )
    //            throws GraphDriverException
    //        {
    //            this.session = driver.createSession( config );
    //            this.driver = driver;
    //            this.root = projectRef;
    //            this.rootSource = source;
    //            this.sources.add( source );
    //        }
    //
    //        public Builder( final EGraphSessionConfiguration config, final EProjectKey key, final EGraphDriver driver )
    //            throws GraphDriverException
    //        {
    //            this.session = driver.createSession( config );
    //            this.root = key.getProject();
    //            this.rootSource = key.getSource();
    //            this.sources.add( key.getSource() );
    //            this.driver = driver;
    //        }
    //
    //        public Builder( final EGraphSession session, final EProjectRelationships rels, final EGraphDriver driver )
    //            throws GraphDriverException
    //        {
    //            this.session = session;
    //            this.driver = driver;
    //            this.root = rels.getKey()
    //                            .getProject();
    //            this.rootSource = rels.getKey()
    //                                  .getSource();
    //            this.sources.add( rels.getKey()
    //                                  .getSource() );
    //            addFromDirectRelationships( rels );
    //        }
    //
    //        public Builder( final EGraphSession session, final URI source, final ProjectVersionRef projectRef,
    //                        final EGraphDriver driver, final String... activeProfiles )
    //            throws GraphDriverException
    //        {
    //            this.session = session;
    //            this.driver = driver;
    //            this.root = projectRef;
    //            this.rootSource = source;
    //            this.sources.add( source );
    //        }
    //
    //        public Builder( final EGraphSession session, final EProjectKey key, final EGraphDriver driver )
    //            throws GraphDriverException
    //        {
    //            this.session = session;
    //            this.root = key.getProject();
    //            this.rootSource = key.getSource();
    //            this.sources.add( key.getSource() );
    //            this.driver = driver;
    //        }
    //
    //        public Builder withFilter( final ProjectRelationshipFilter filter )
    //        {
    //            this.filter = filter;
    //            return this;
    //        }
    //
    //        public Builder withParent( final ParentRelationship parent )
    //        {
    //            if ( parent.getDeclaring()
    //                       .equals( root ) )
    //            {
    //                relationships.add( parent );
    //            }
    //            else
    //            {
    //                relationships.add( parent.cloneFor( root ) );
    //            }
    //            return this;
    //        }
    //
    //        public Builder withDirectProjectRelationships( final EProjectRelationships... rels )
    //        {
    //            return withDirectProjectRelationships( Arrays.asList( rels ) );
    //        }
    //
    //        public Builder withDirectProjectRelationships( final Collection<EProjectRelationships> rels )
    //        {
    //            for ( final EProjectRelationships relationships : rels )
    //            {
    //                this.sources.add( relationships.getKey()
    //                                               .getSource() );
    //                if ( relationships.getKey()
    //                                  .getProject()
    //                                  .equals( root ) )
    //                {
    //                    addFromDirectRelationships( relationships );
    //                }
    //                else
    //                {
    //                    this.projects.add( relationships );
    //                }
    //            }
    //
    //            return this;
    //        }
    //
    //        private void addFromDirectRelationships( final EProjectRelationships relationships )
    //        {
    //            this.relationships.clear();
    //            this.relationships.add( relationships.getParent() );
    //            this.relationships.addAll( relationships.getDependencies() );
    //            this.relationships.addAll( relationships.getManagedDependencies() );
    //
    //            this.relationships.addAll( relationships.getPlugins() );
    //            this.relationships.addAll( relationships.getManagedPlugins() );
    //
    //            this.relationships.addAll( relationships.getExtensions() );
    //
    //            if ( relationships.getPluginDependencies() != null )
    //            {
    //                for ( final Map.Entry<PluginRelationship, List<PluginDependencyRelationship>> entry : relationships.getPluginDependencies()
    //                                                                                                                   .entrySet() )
    //                {
    //                    if ( entry.getValue() != null )
    //                    {
    //                        this.relationships.addAll( entry.getValue() );
    //                    }
    //                }
    //            }
    //        }
    //
    //        public Builder withDependencies( final List<DependencyRelationship> rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( rels );
    //            return this;
    //        }
    //
    //        private void addSources( final Iterable<? extends ProjectRelationship<?>> rels )
    //        {
    //            for ( final ProjectRelationship<?> rel : rels )
    //            {
    //                this.sources.addAll( rel.getSources() );
    //            }
    //        }
    //
    //        private <T extends ProjectRelationship<?>> void addSources( final T... rels )
    //        {
    //            for ( final ProjectRelationship<?> rel : rels )
    //            {
    //                this.sources.addAll( rel.getSources() );
    //            }
    //        }
    //
    //        public Builder withDependencies( final DependencyRelationship... rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( Arrays.asList( rels ) );
    //            return this;
    //        }
    //
    //        public Builder withPlugins( final Collection<PluginRelationship> rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( rels );
    //            return this;
    //        }
    //
    //        public Builder withPlugins( final PluginRelationship... rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( Arrays.asList( rels ) );
    //            return this;
    //        }
    //
    //        public Builder withPluginLevelDependencies( final Collection<PluginDependencyRelationship> rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( rels );
    //            return this;
    //        }
    //
    //        public Builder withPluginLevelDependencies( final PluginDependencyRelationship... rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( Arrays.asList( rels ) );
    //            return this;
    //        }
    //
    //        public Builder withExtensions( final Collection<ExtensionRelationship> rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( rels );
    //            return this;
    //        }
    //
    //        public Builder withExtensions( final ExtensionRelationship... rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( Arrays.asList( rels ) );
    //            return this;
    //        }
    //
    //        public Builder withExactRelationships( final Collection<ProjectRelationship<?>> rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( rels );
    //            return this;
    //        }
    //
    //        public Builder withExactRelationships( final ProjectRelationship<?>... rels )
    //        {
    //            addSources( rels );
    //            this.relationships.addAll( Arrays.asList( rels ) );
    //            return this;
    //        }
    //
    //        public Builder withRelationships( final Collection<ProjectRelationship<?>> rels )
    //        {
    //            addSources( rels );
    //            final Set<PluginDependencyRelationship> pluginDepRels = new HashSet<PluginDependencyRelationship>();
    //            for ( final ProjectRelationship<?> rel : rels )
    //            {
    //                switch ( rel.getType() )
    //                {
    //                    case DEPENDENCY:
    //                    {
    //                        final DependencyRelationship dr = (DependencyRelationship) rel;
    //                        withDependencies( dr );
    //
    //                        break;
    //                    }
    //                    case PLUGIN:
    //                    {
    //                        final PluginRelationship pr = (PluginRelationship) rel;
    //                        withPlugins( pr );
    //
    //                        break;
    //                    }
    //                    case EXTENSION:
    //                    {
    //                        withExtensions( (ExtensionRelationship) rel );
    //                        break;
    //                    }
    //                    case PLUGIN_DEP:
    //                    {
    //                        // load all plugin relationships first.
    //                        pluginDepRels.add( (PluginDependencyRelationship) rel );
    //                        break;
    //                    }
    //                    case PARENT:
    //                    {
    //                        withParent( (ParentRelationship) rel );
    //                        break;
    //                    }
    //                }
    //            }
    //
    //            withPluginLevelDependencies( pluginDepRels );
    //
    //            return this;
    //        }
    //
    //        public EProjectGraph build()
    //            throws GraphDriverException
    //        {
    //            boolean foundParent = false;
    //            for ( final ProjectRelationship<?> rel : relationships )
    //            {
    //                if ( rel instanceof ParentRelationship && rel.getDeclaring()
    //                                                             .equals( root ) )
    //                {
    //                    foundParent = true;
    //                    break;
    //                }
    //            }
    //
    //            final EProjectKey key = new EProjectKey( rootSource, root );
    //            if ( !foundParent )
    //            {
    //                relationships.add( new ParentRelationship( key.getSource(), key.getProject() ) );
    //            }
    //
    //            return new EProjectGraph( session, key, relationships, projects, cycles, filter, driver );
    //        }
    //
    //        public Builder withCycles( final Set<EProjectCycle> cycles )
    //        {
    //            if ( cycles != null )
    //            {
    //                this.cycles = cycles;
    //            }
    //
    //            return this;
    //        }
    //
    //    }

    public Set<ProjectRelationship<?>> add( final EProjectDirectRelationships rels )
        throws GraphDriverException
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
        driver.traverse( view, traversal, this, ref );
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
        final Collection<? extends ProjectRelationship<?>> rels = driver.getRelationshipsTargeting( view, ref );
        if ( rels == null )
        {
            return null;
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
        driver.setMetadata( key.getProject(), metadata );
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
        return driver.getAllPathsTo( view, refs );
    }

    @Override
    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return driver.introducesCycle( view, rel );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public EProjectGraph filteredInstance( final ProjectRelationshipFilter filter )
        throws GraphDriverException
    {
        return new EProjectGraph( view.getWorkspace(), driver, filter, project );
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        driver.addDisconnectedProject( ref );
    }

    @Override
    public String toString()
    {
        return String.format( "EProjectGraph [root: %s, session: %s]", project, view.getWorkspace() );
    }

    @Override
    public boolean isMissing( final ProjectVersionRef ref )
    {
        return driver.isMissing( view, ref );
    }

}
