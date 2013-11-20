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
package org.commonjava.maven.atlas.graph.util;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.artifact;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.AbstractAggregatingFilter;
import org.commonjava.maven.atlas.graph.filter.AbstractTypedFilter;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.util.logging.Logger;

public final class RelationshipUtils
{

    private RelationshipUtils()
    {
    }

    public static final URI UNKNOWN_SOURCE_URI;

    public static final URI POM_ROOT_URI;

    public static URI ANY_SOURCE_URI;

    static
    {
        try
        {
            ANY_SOURCE_URI = new URI( "any:any" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct any-source URI: 'any:any'" );
        }

        try
        {
            UNKNOWN_SOURCE_URI = new URI( "unknown:unknown" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct unknown-source URI: 'unknown:unknown'" );
        }

        try
        {
            POM_ROOT_URI = new URI( "pom:root" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct pom-root URI: 'pom:root'" );
        }
    }

    public static Map<ProjectVersionRef, List<ProjectRelationship<?>>> mapByDeclaring( final Collection<ProjectRelationship<?>> relationships )
    {
        final Logger logger = new Logger( RelationshipUtils.class );
        logger.info( "Mapping %d relationships by declaring GAV:\n\n  %s\n\n", relationships.size(), join( relationships, "\n  " ) );
        final Map<ProjectVersionRef, List<ProjectRelationship<?>>> result = new HashMap<ProjectVersionRef, List<ProjectRelationship<?>>>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            final ProjectVersionRef declaring = rel.getDeclaring();
            List<ProjectRelationship<?>> outbound = result.get( declaring );
            if ( outbound == null )
            {
                outbound = new ArrayList<ProjectRelationship<?>>();
                result.put( rel.getDeclaring(), outbound );
            }

            if ( !outbound.contains( rel ) )
            {
                outbound.add( rel );
            }
        }

        return result;

    }

    public static URI profileLocation( final String profile )
    {
        if ( profile == null || profile.trim()
                                       .length() < 1 )
        {
            return POM_ROOT_URI;
        }

        try
        {
            return new URI( "pom:profile:" + profile );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct pom-profile URI: 'pom:profile:" + profile + "'" );
        }
    }

    public static void filterTerminalParents( final Collection<ProjectRelationship<?>> rels )
    {
        for ( final Iterator<ProjectRelationship<?>> it = rels.iterator(); it.hasNext(); )
        {
            final ProjectRelationship<?> rel = it.next();
            if ( ( rel instanceof ParentRelationship ) && ( (ParentRelationship) rel ).isTerminus() )
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

    public static void filter( final Set<ProjectRelationship<?>> rels, final ProjectRelationshipFilter filter )
    {
        if ( filter == null || filter instanceof AnyFilter )
        {
            return;
        }

        if ( rels == null || rels.isEmpty() )
        {
            return;
        }

        for ( final Iterator<ProjectRelationship<?>> iterator = rels.iterator(); iterator.hasNext(); )
        {
            final ProjectRelationship<?> rel = iterator.next();
            if ( !filter.accept( rel ) )
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

    public static Set<ProjectVersionRef> targets( final ProjectRelationship<?>... relationships )
    {
        return targets( Arrays.asList( relationships ) );
    }

    public static Set<ProjectVersionRef> targets( final Collection<ProjectRelationship<?>> relationships )
    {
        final Set<ProjectVersionRef> results = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            results.add( rel.getTarget() );
        }

        return results;
    }

    public static Set<ProjectVersionRef> gavs( final ProjectRelationship<?>... relationships )
    {
        return gavs( Arrays.asList( relationships ) );
    }

    public static Set<ProjectVersionRef> gavs( final Collection<ProjectRelationship<?>> relationships )
    {
        final Set<ProjectVersionRef> results = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : relationships )
        {
            results.add( rel.getDeclaring()
                            .asProjectVersionRef() );

            results.add( rel.getTarget()
                            .asProjectVersionRef() );
        }

        return results;
    }

    public static ExtensionRelationship extension( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                   final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return new ExtensionRelationship( source, owner, projectVersion( groupId, artifactId, version ), index );
    }

    public static PluginRelationship plugin( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                             final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return plugin( source, pomLocation, owner, groupId, artifactId, version, index, false );
    }

    public static PluginRelationship plugin( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                             final String artifactId, final String version, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( source, pomLocation, owner, projectVersion( groupId, artifactId, version ), index, managed );
    }

    public static PluginRelationship plugin( final URI source, final URI pomLocation, final ProjectVersionRef owner, final ProjectVersionRef plugin,
                                             final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( source, pomLocation, owner, plugin, index, managed );
    }

    public static PluginRelationship plugin( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                             final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return plugin( source, owner, groupId, artifactId, version, index, false );
    }

    public static PluginRelationship plugin( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                             final String version, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( source, owner, projectVersion( groupId, artifactId, version ), index, managed );
    }

    public static PluginRelationship plugin( final URI source, final ProjectVersionRef owner, final ProjectVersionRef plugin, final int index,
                                             final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( source, owner, plugin, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final ProjectVersionRef owner, final ProjectRef plugin,
                                                                 final String groupId, final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( source, owner, plugin, groupId, artifactId, version, null, null, index, false );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final ProjectVersionRef owner, final ProjectRef plugin,
                                                                 final String groupId, final String artifactId, final String version,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( source, owner, plugin, groupId, artifactId, version, null, null, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final ProjectVersionRef owner, final ProjectRef plugin,
                                                                 final String groupId, final String artifactId, final String version,
                                                                 final String type, final String classifier, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( source, owner, plugin, artifact( groupId, artifactId, version, type, classifier, false ), index,
                                                 managed );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final ProjectVersionRef owner, final ProjectRef plugin,
                                                                 final ProjectVersionRef dep, final String type, final String classifier,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( source, owner, plugin, artifact( dep, type, classifier, false ), index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final String groupId, final String artifactId,
                                                                 final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( source, pomLocation, owner, plugin, groupId, artifactId, version, null, null, index, false );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final String groupId, final String artifactId,
                                                                 final String version, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( source, pomLocation, owner, plugin, groupId, artifactId, version, null, null, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final String groupId, final String artifactId,
                                                                 final String version, final String type, final String classifier, final int index,
                                                                 final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( source, pomLocation, owner, plugin,
                                                 artifact( groupId, artifactId, version, type, classifier, false ), index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                                 final ProjectRef plugin, final ProjectVersionRef dep, final String type,
                                                                 final String classifier, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( source, pomLocation, owner, plugin, artifact( dep, type, classifier, false ), index, managed );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                     final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( source, owner, groupId, artifactId, version, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final ProjectVersionRef dep, final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( source, owner, dep, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                     final String version, final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return dependency( source, owner, groupId, artifactId, version, null, null, false, scope, index, managed );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final ProjectVersionRef dep,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( source, owner, artifact( dep, null, null, false ), scope, index, managed );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                     final String version, final String type, final String classifier, final boolean optional,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( source, owner, artifact( groupId, artifactId, version, type, classifier, optional ), null, index, false );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final ProjectVersionRef dep, final String type,
                                                     final String classifier, final boolean optional, final DependencyScope scope, final int index,
                                                     final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( source, owner, artifact( dep, type, classifier, optional ), null, index, false );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( source, pomLocation, owner, groupId, artifactId, version, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( source, pomLocation, owner, dep, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final DependencyScope scope, final int index,
                                                     final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return dependency( source, pomLocation, owner, groupId, artifactId, version, null, null, false, scope, index, managed );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( source, pomLocation, owner, artifact( dep, null, null, false ), scope, index, managed );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final String type, final String classifier,
                                                     final boolean optional, final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( source, pomLocation, owner, artifact( groupId, artifactId, version, type, classifier, optional ), null,
                                           index, false );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final String type, final String classifier, final boolean optional,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( source, pomLocation, owner, artifact( dep, type, classifier, optional ), null, index, false );
    }

    public static Set<RelationshipType> getRelationshipTypes( final ProjectRelationshipFilter filter )
    {
        if ( filter == null )
        {
            return new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) );
        }

        final Set<RelationshipType> result = new HashSet<RelationshipType>();

        if ( filter instanceof AbstractTypedFilter )
        {
            final AbstractTypedFilter typedFilter = (AbstractTypedFilter) filter;
            result.addAll( typedFilter.getRelationshipTypes() );
            result.addAll( typedFilter.getDescendantRelationshipTypes() );
        }
        else if ( filter instanceof AbstractAggregatingFilter )
        {
            final List<? extends ProjectRelationshipFilter> filters = ( (AbstractAggregatingFilter) filter ).getFilters();

            for ( final ProjectRelationshipFilter f : filters )
            {
                result.addAll( getRelationshipTypes( f ) );
            }
        }
        else
        {
            result.addAll( Arrays.asList( RelationshipType.values() ) );
        }

        return result;
    }

}
