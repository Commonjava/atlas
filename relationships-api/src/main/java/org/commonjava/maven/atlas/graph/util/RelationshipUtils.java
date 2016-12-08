/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.graph.util;

import static org.commonjava.maven.atlas.ident.util.IdentityUtils.artifact;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

public final class RelationshipUtils
{

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                     final String version, final int index, final boolean inherited, final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return dependency( source, owner, groupId, artifactId, version, null, null, optional, null, index, false, inherited );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final int index, final boolean inherited,
                                                     final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return dependency( source, owner, dep, null, null, optional, null, index, false, inherited );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                     final String version, final DependencyScope scope, final int index, final boolean managed,
                                                     final boolean inherited, final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return dependency( source, owner, groupId, artifactId, version, null, null, optional, scope, index, managed, inherited );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final ProjectVersionRef dep,
                                                     final DependencyScope scope, final int index, final boolean managed,
                                                     final boolean inherited, final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return new SimpleDependencyRelationship( source, owner, artifact( dep, null, null ), scope, index, managed, inherited, optional );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final String groupId, final String artifactId,
                                                     final String version, final String type, final String classifier, final boolean optional,
                                                     final DependencyScope scope, final int index, final boolean managed, final boolean inherited )
            throws InvalidVersionSpecificationException
    {
        return new SimpleDependencyRelationship( source, owner, artifact( groupId, artifactId, version, type, classifier ), scope, index,
                managed, inherited, optional );
    }

    public static DependencyRelationship dependency( final URI source, final ProjectVersionRef owner, final ProjectVersionRef dep, final String type,
                                                     final String classifier, final boolean optional, final DependencyScope scope, final int index,
                                                     final boolean managed, final boolean inherited )
            throws InvalidVersionSpecificationException
    {
        return new SimpleDependencyRelationship( source, owner, artifact( dep, type, classifier ), scope, index, managed, inherited, optional );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final int index, final boolean inherited,
                                                     final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return dependency( source, pomLocation, owner, groupId, artifactId, version, null, null, optional, null, index, false, inherited );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final int index, final boolean inherited,
                                                     final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return dependency( source, pomLocation, owner, dep, null, null, optional, null, index, false, inherited );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final DependencyScope scope, final int index,
                                                     final boolean managed, final boolean inherited, final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return dependency( source, pomLocation, owner, groupId, artifactId, version, null, null, optional, scope, index, managed, inherited );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final DependencyScope scope, final int index,
                                                     final boolean managed, final boolean inherited, final boolean optional )
            throws InvalidVersionSpecificationException
    {
        return new SimpleDependencyRelationship( source, pomLocation, owner, artifact( dep, null, null ), scope, index, managed, inherited, optional );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner, final String groupId,
                                                     final String artifactId, final String version, final String type, final String classifier,
                                                     final boolean optional, final DependencyScope scope, final int index, final boolean managed,
                                                     final boolean inherited )
            throws InvalidVersionSpecificationException
    {
        return new SimpleDependencyRelationship( source, pomLocation, owner, artifact( groupId, artifactId, version, type, classifier ), scope,
                index, managed, inherited, optional );
    }

    public static DependencyRelationship dependency( final URI source, final URI pomLocation, final ProjectVersionRef owner,
                                                     final ProjectVersionRef dep, final String type, final String classifier, final boolean optional,
                                                     final DependencyScope scope, final int index, final boolean managed, final boolean inherited )
            throws InvalidVersionSpecificationException
    {
        return new SimpleDependencyRelationship( source, pomLocation, owner, artifact( dep, type, classifier ), scope, index, managed, inherited, optional );
    }

    public static void filterTerminalParents( final Collection<? extends ProjectRelationship<?, ?>> rels )
    {
        for (final Iterator<? extends ProjectRelationship<?, ?>> it = rels.iterator(); it.hasNext(); )
        {
            final ProjectRelationship<?, ?> rel = it.next();
            if ( ( rel instanceof SimpleParentRelationship) && ( (ParentRelationship) rel ).isTerminus() )
            {
                it.remove();
            }
        }
    }

}
