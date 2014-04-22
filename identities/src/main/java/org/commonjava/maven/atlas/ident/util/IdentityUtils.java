/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.ident.util;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

public final class IdentityUtils
{

    private IdentityUtils()
    {
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

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version, final String type,
                                        final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( projectVersion( groupId, artifactId, version ), type, classifier, optional );
    }

    public static ArtifactRef artifact( final ProjectVersionRef dep, final String type, final String classifier, final boolean optional )
    {
        return new ArtifactRef( dep, type, classifier, optional );
    }

    public static ProjectVersionRef projectVersion( final String src )
    {
        final String[] parts = src.split( ":" );
        if ( parts.length != 3 )
        {
            throw new IllegalArgumentException( "Invalid: '" + src + "'. Must contain exactly three fields separated by ':'" );
        }

        try
        {
            return new ProjectVersionRef( parts[0], parts[1], parts[2] );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new IllegalArgumentException( "Invalid: '" + src + "'. Version: '" + parts[2] + "' is invalid: " + e.getMessage(), e );
        }
    }

    public static ProjectVersionRef projectVersion( final String groupId, final String artifactId, final String version )
        throws InvalidVersionSpecificationException
    {
        return new ProjectVersionRef( groupId, artifactId, version );
    }

    public static ProjectRef project( final String src )
    {
        final String[] parts = src.split( ":" );
        if ( parts.length < 2 )
        {
            throw new IllegalArgumentException( "Invalid: '" + src + "'. Must contain at least two fields separated by ':'" );
        }

        return new ProjectRef( parts[0], parts[1] );
    }

    public static ProjectRef project( final String groupId, final String artifactId )
        throws InvalidVersionSpecificationException
    {
        return new ProjectRef( groupId, artifactId );
    }

}
