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

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.filterTerminalParents;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class EProjectDirectRelationships
    implements EProjectRelationshipCollection, Serializable
{

    private static final long serialVersionUID = 1L;

    private final EProjectKey key;

    private final List<DependencyRelationship> dependencies;

    private final List<DependencyRelationship> managedDependencies;

    private final List<PluginRelationship> plugins;

    private final List<PluginRelationship> managedPlugins;

    private final List<ExtensionRelationship> extensions;

    private final ParentRelationship parent;

    private final Map<PluginRelationship, List<PluginDependencyRelationship>> pluginDependencies;

    public EProjectDirectRelationships( final EProjectKey key,
                                        final ParentRelationship parent,
                                        final List<DependencyRelationship> dependencies,
                                        final List<PluginRelationship> plugins,
                                        final List<DependencyRelationship> managedDependencies,
                                        final List<PluginRelationship> managedPlugins,
                                        final List<ExtensionRelationship> extensions,
                                        final Map<PluginRelationship, List<PluginDependencyRelationship>> pluginDependencies )
    {
        this.key = key;
        this.parent = parent;
        this.dependencies = Collections.unmodifiableList( dependencies );
        this.plugins = Collections.unmodifiableList( plugins );
        this.managedDependencies = Collections.unmodifiableList( managedDependencies );
        this.managedPlugins = Collections.unmodifiableList( managedPlugins );
        this.extensions = Collections.unmodifiableList( extensions );

        final HashMap<PluginRelationship, List<PluginDependencyRelationship>> pdrels =
            new HashMap<PluginRelationship, List<PluginDependencyRelationship>>();

        for ( final Map.Entry<PluginRelationship, List<PluginDependencyRelationship>> entry : pluginDependencies.entrySet() )
        {
            pdrels.put( entry.getKey(), Collections.unmodifiableList( entry.getValue() ) );
        }

        this.pluginDependencies = Collections.unmodifiableMap( pdrels );
    }

    public EProjectKey getKey()
    {
        return key;
    }

    public final ProjectVersionRef getProjectRef()
    {
        return key.getProject();
    }

    public final List<DependencyRelationship> getDependencies()
    {
        return dependencies;
    }

    public final List<DependencyRelationship> getManagedDependencies()
    {
        return managedDependencies;
    }

    public final List<PluginRelationship> getPlugins()
    {
        return plugins;
    }

    public final List<PluginRelationship> getManagedPlugins()
    {
        return managedPlugins;
    }

    public final List<ExtensionRelationship> getExtensions()
    {
        return extensions;
    }

    public final ParentRelationship getParent()
    {
        return parent;
    }

    public final Map<PluginRelationship, List<PluginDependencyRelationship>> getPluginDependencies()
    {
        return pluginDependencies;
    }

    public final List<PluginDependencyRelationship> getPluginDependencies( final ProjectVersionRef plugin,
                                                                           final boolean managed )
    {
        final PluginRelationship pr = new PluginRelationship( key.getSource(), getProjectRef(), plugin, 0, managed );
        return pluginDependencies.get( pr );
    }

    @Override
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    @Override
    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
        if ( parent != null )
        {
            result.add( parent );
        }

        result.addAll( dependencies );
        result.addAll( managedDependencies );
        result.addAll( plugins );
        result.addAll( managedPlugins );
        result.addAll( extensions );

        for ( final List<PluginDependencyRelationship> pluginRels : pluginDependencies.values() )
        {
            result.addAll( pluginRels );
        }

        return result;
    }

    public static final class Builder
    {
        private final EProjectKey key;

        private final List<DependencyRelationship> dependencies = new ArrayList<DependencyRelationship>();

        private final List<DependencyRelationship> managedDependencies = new ArrayList<DependencyRelationship>();

        private final List<PluginRelationship> plugins = new ArrayList<PluginRelationship>();

        private final List<PluginRelationship> managedPlugins = new ArrayList<PluginRelationship>();

        private final List<ExtensionRelationship> extensions = new ArrayList<ExtensionRelationship>();

        private ParentRelationship parent;

        private final Map<PluginRelationship, List<PluginDependencyRelationship>> pluginDependencies =
            new HashMap<PluginRelationship, List<PluginDependencyRelationship>>();

        public Builder( final URI source, final ProjectVersionRef projectRef, final String... activeProfiles )
        {
            this.key = new EProjectKey( source, projectRef );
        }

        public Builder( final EProjectKey key )
        {
            this.key = key;
        }

        public EProjectDirectRelationships build()
        {
            if ( parent == null )
            {
                parent = new ParentRelationship( key.getSource(), key.getProject() );
            }

            return new EProjectDirectRelationships( key, parent, dependencies, plugins, managedDependencies,
                                                    managedPlugins, extensions, pluginDependencies );
        }

        public Builder withParent( final ProjectVersionRef parent )
        {
            this.parent = new ParentRelationship( key.getSource(), key.getProject(), parent );
            return this;
        }

        public Builder withParent( final ParentRelationship parent )
        {
            this.parent = adjustDeclaring( parent );

            return this;
        }

        @SuppressWarnings( "unchecked" )
        private <C extends ProjectRelationship<?>> C adjustDeclaring( final C rel )
        {
            if ( !key.getProject()
                     .equals( rel.getDeclaring() ) )
            {
                return (C) rel.cloneFor( key.getProject() );
            }

            return rel;
        }

        public Builder withDependencies( final DependencyRelationship... deps )
        {
            return withDependencies( Arrays.asList( deps ) );
        }

        public Builder withDependencies( final Collection<DependencyRelationship> deps )
        {
            for ( DependencyRelationship dep : deps )
            {
                dep = adjustDeclaring( dep );
                if ( dep.isManaged() )
                {
                    if ( !managedDependencies.contains( dep ) )
                    {
                        managedDependencies.add( dep );
                    }
                }
                else
                {
                    if ( !dependencies.contains( dep ) )
                    {
                        dependencies.add( dep );
                    }
                }
            }

            return this;
        }

        public Builder withPlugins( final PluginRelationship... plugins )
        {
            return withPlugins( Arrays.asList( plugins ) );
        }

        public Builder withPlugins( final Collection<PluginRelationship> plugins )
        {
            for ( PluginRelationship plugin : plugins )
            {
                plugin = adjustDeclaring( plugin );
                if ( plugin.isManaged() )
                {
                    if ( !managedPlugins.contains( plugin ) )
                    {
                        managedPlugins.add( plugin );
                    }
                }
                else
                {
                    if ( !this.plugins.contains( plugin ) )
                    {
                        this.plugins.add( plugin );
                    }
                }
            }

            return this;
        }

        public Builder withPluginDependencies( final PluginDependencyRelationship... pluginDeps )
        {
            return withPluginDependencies( Arrays.asList( pluginDeps ) );
        }

        public Builder withPluginDependencies( final Collection<PluginDependencyRelationship> pluginDeps )
        {
            for ( PluginDependencyRelationship rel : pluginDeps )
            {
                rel = adjustDeclaring( rel );
                final ProjectRef pluginRef = rel.getPlugin();

                PluginRelationship pr = null;
                if ( rel.isManaged() )
                {
                    for ( final PluginRelationship pluginRel : managedPlugins )
                    {
                        if ( pluginRef.equals( pluginRel.getTarget() ) )
                        {
                            pr = pluginRel;
                            break;
                        }
                    }
                }
                else
                {
                    for ( final PluginRelationship pluginRel : plugins )
                    {
                        if ( pluginRef.equals( pluginRel.getTarget() ) )
                        {
                            pr = pluginRel;
                            break;
                        }
                    }
                }

                if ( pr == null )
                {
                    throw new IllegalArgumentException(
                                                        "Orphaned plugin-level dependency found: "
                                                            + rel
                                                            + ". Make sure you load plugin relationships BEFORE attempting to load plugin-dependency-relationships." );
                }

                List<PluginDependencyRelationship> pdrs = pluginDependencies.get( pr );
                if ( pdrs == null )
                {
                    pdrs = new ArrayList<PluginDependencyRelationship>();
                    pluginDependencies.put( pr, pdrs );
                }

                if ( !pdrs.contains( rel ) )
                {
                    pdrs.add( rel );
                }
            }

            return this;
        }

        public Builder withExtensions( final ExtensionRelationship... exts )
        {
            return withExtensions( Arrays.asList( exts ) );
        }

        public Builder withExtensions( final Collection<ExtensionRelationship> exts )
        {
            for ( final ExtensionRelationship ext : exts )
            {
                if ( !extensions.contains( ext ) )
                {
                    extensions.add( adjustDeclaring( ext ) );
                }
            }

            return this;
        }

        public Builder withRelationships( final Collection<ProjectRelationship<?>> relationships )
        {
            final Set<PluginDependencyRelationship> pluginDepRels = new HashSet<PluginDependencyRelationship>();
            for ( ProjectRelationship<?> rel : relationships )
            {
                rel = adjustDeclaring( rel );
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

            withPluginDependencies( pluginDepRels );

            return this;
        }

        public int getNextPluginIndex( final boolean managed )
        {
            return managed ? managedPlugins.size() : plugins.size();
        }

        public int getNextPluginDependencyIndex( final ProjectVersionRef plugin, final boolean managed )
        {
            final List<PluginDependencyRelationship> list =
                pluginDependencies.get( new PluginRelationship( key.getSource(), key.getProject(), plugin, 0, managed ) );
            return list == null ? 0 : list.size();
        }

        public int getNextDependencyIndex( final boolean managed )
        {
            return managed ? managedDependencies.size() : dependencies.size();
        }

        public int getNextExtensionIndex()
        {
            return extensions.size();
        }

        public Builder withDependency( final ProjectVersionRef ref, final String type, final String classifier,
                                       final DependencyScope scope, final boolean managed )
        {
            withDependencies( new DependencyRelationship( key.getSource(), key.getProject(),
                                                          new ArtifactRef( ref, type, classifier, false ), scope,
                                                          getNextDependencyIndex( managed ), managed ) );

            return this;
        }

        public Builder withPlugin( final ProjectVersionRef ref, final boolean managed )
        {
            withPlugins( new PluginRelationship( key.getSource(), key.getProject(), ref, getNextPluginIndex( managed ),
                                                 managed ) );

            return this;
        }

        public Builder withExtension( final ProjectVersionRef ref )
        {
            withExtensions( new ExtensionRelationship( key.getSource(), key.getProject(), ref, getNextExtensionIndex() ) );
            return this;
        }

        public ProjectVersionRef getProjectRef()
        {
            return key.getProject();
        }

    }

}
