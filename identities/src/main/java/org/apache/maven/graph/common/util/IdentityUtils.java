package org.apache.maven.graph.common.util;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;

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

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version,
                                        final String type, final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( projectVersion( groupId, artifactId, version ), type, classifier, optional );
    }

    public static ArtifactRef artifact( final ProjectVersionRef dep, final String type, final String classifier,
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
