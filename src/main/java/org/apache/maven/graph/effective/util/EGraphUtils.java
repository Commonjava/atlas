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
package org.apache.maven.graph.effective.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public final class EGraphUtils
{

    private EGraphUtils()
    {
    }

    public static void filterTerminalParents( final Collection<ProjectRelationship<?>> rels )
    {
        for ( final Iterator<ProjectRelationship<?>> it = rels.iterator(); it.hasNext(); )
        {
            final ProjectRelationship<?> rel = it.next();
            if ( ( rel instanceof ParentRelationship ) && rel.getDeclaring()
                                                             .equals( rel.getTarget() ) )
            {
                it.remove();
            }
        }
    }

    public static void filter( final Set<ProjectRelationship<?>> rels, final RelationshipType... types )
    {
        if ( rels == null || rels.isEmpty() )
        {
            return;
        }

        if ( types == null || types.length < 1 )
        {
            return;
        }

        Arrays.sort( types );
        for ( final Iterator<ProjectRelationship<?>> iterator = rels.iterator(); iterator.hasNext(); )
        {
            final ProjectRelationship<?> rel = iterator.next();
            if ( Arrays.binarySearch( types, rel.getType() ) < 0 )
            {
                iterator.remove();
            }
        }
    }

    public static Set<ProjectVersionRef> declarers( final ProjectRelationship<?>... relationships )
    {
        return declarers( Arrays.asList( relationships ) );
    }

    public static Set<ProjectVersionRef> declarers( final Collection<ProjectRelationship<?>> relationships )
    {
        final Set<ProjectVersionRef> results = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            results.add( rel.getDeclaring() );
        }

        return results;
    }

    public static Set<ProjectRef> targets( final ProjectRelationship<?>... relationships )
    {
        return targets( Arrays.asList( relationships ) );
    }

    public static Set<ProjectRef> targets( final Collection<ProjectRelationship<?>> relationships )
    {
        final Set<ProjectRef> results = new HashSet<ProjectRef>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            results.add( rel.getTarget() );
        }

        return results;
    }

    public static ExtensionRelationship extension( final ProjectVersionRef owner, final String groupId,
                                                   final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return new ExtensionRelationship( owner, projectVersion( groupId, artifactId, version ), index );
    }

    public static PluginRelationship plugin( final ProjectVersionRef owner, final String groupId,
                                             final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return plugin( owner, groupId, artifactId, version, index, false );
    }

    public static PluginRelationship plugin( final ProjectVersionRef owner, final String groupId,
                                             final String artifactId, final String version, final int index,
                                             final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( owner, projectVersion( groupId, artifactId, version ), index, managed );
    }

    public static PluginRelationship plugin( final ProjectVersionRef owner, final ProjectVersionRef plugin,
                                             final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( owner, plugin, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final String groupId,
                                                                 final String artifactId, final String version,
                                                                 final int index )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( owner, plugin, groupId, artifactId, version, null, null, index, false );
    }

    public static PluginDependencyRelationship pluginDependency( final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final String groupId,
                                                                 final String artifactId, final String version,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( owner, plugin, groupId, artifactId, version, null, null, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final String groupId,
                                                                 final String artifactId, final String version,
                                                                 final String type, final String classifier,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( owner, plugin, artifact( groupId, artifactId, version, type,
                                                                          classifier, false ), index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final ProjectVersionRef dep,
                                                                 final String type, final String classifier,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( owner, plugin, artifact( dep, type, classifier, false ), index,
                                                 managed );
    }

    public static DependencyRelationship dependency( final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( owner, groupId, artifactId, version, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final ProjectVersionRef owner, final ProjectVersionRef dep,
                                                     final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( owner, dep, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return dependency( owner, groupId, artifactId, version, null, null, false, scope, index, managed );
    }

    public static DependencyRelationship dependency( final ProjectVersionRef owner, final ProjectVersionRef dep,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( owner, artifact( dep, null, null, false ), scope, index, managed );
    }

    public static DependencyRelationship dependency( final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final String type,
                                                     final String classifier, final boolean optional,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( owner, artifact( groupId, artifactId, version, type, classifier, optional ),
                                           null, index, false );
    }

    public static DependencyRelationship dependency( final ProjectVersionRef owner, final ProjectVersionRef dep,
                                                     final String type, final String classifier,
                                                     final boolean optional, final DependencyScope scope,
                                                     final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( owner, artifact( dep, type, classifier, optional ), null, index, false );
    }

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( projectVersion( groupId, artifactId, version ), null, null, false );
    }

    public static ArtifactRef artifact( final ProjectVersionRef ref )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( ref, null, null, false );
    }

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version,
                                        final String type, final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( projectVersion( groupId, artifactId, version ), type, classifier, optional );
    }

    private static ArtifactRef artifact( final ProjectVersionRef dep, final String type, final String classifier,
                                         final boolean optional )
    {
        return new ArtifactRef( dep, type, classifier, optional );
    }

    public static ProjectVersionRef projectVersion( final String src )
    {
        final String[] parts = src.split( ":" );
        if ( parts.length != 3 )
        {
            throw new IllegalArgumentException( "Invalid: '" + src
                + "'. Must contain exactly three fields separated by ':'" );
        }

        try
        {
            return new ProjectVersionRef( parts[0], parts[1], parts[2] );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new IllegalArgumentException( "Invalid: '" + src + "'. Version: '" + parts[2] + "' is invalid: "
                + e.getMessage(), e );
        }
    }

    public static ProjectVersionRef projectVersion( final String groupId, final String artifactId, final String version )
        throws InvalidVersionSpecificationException
    {
        return new ProjectVersionRef( groupId, artifactId, version );
    }

}
