/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.ident.util;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.maven.ident.version.InvalidVersionSpecificationException;

public final class IdentityUtils
{

    private IdentityUtils()
    {
    }

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version )
        throws InvalidVersionSpecificationException
    {
        return new SimpleArtifactRef( projectVersion( groupId, artifactId, version ), null, null );
    }

    public static ArtifactRef artifact( final ProjectVersionRef ref )
        throws InvalidVersionSpecificationException
    {
        return new SimpleArtifactRef( ref, null, null );
    }

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version,
                                        final String type, final String classifier )
        throws InvalidVersionSpecificationException
    {
        return new SimpleArtifactRef( projectVersion( groupId, artifactId, version ), type, classifier );
    }

    public static ArtifactRef artifact( final ProjectVersionRef dep, final String type, final String classifier )
    {
        return new SimpleArtifactRef( dep, type, classifier );
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
            return new SimpleProjectVersionRef( parts[0], parts[1], parts[2] );
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
        return new SimpleProjectVersionRef( groupId, artifactId, version );
    }

    public static ProjectRef project( final String src )
    {
        final String[] parts = src.split( ":" );
        if ( parts.length < 2 )
        {
            throw new IllegalArgumentException( "Invalid: '" + src
                + "'. Must contain at least two fields separated by ':'" );
        }

        return new SimpleProjectRef( parts[0], parts[1] );
    }

    public static ProjectRef project( final String groupId, final String artifactId )
        throws InvalidVersionSpecificationException
    {
        return new SimpleProjectRef( groupId, artifactId );
    }

}
