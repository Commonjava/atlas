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
package org.commonjava.maven.atlas.ident.ref;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.Serializable;

import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.atlas.ident.version.VersionUtils;

public class ProjectVersionRef
    extends ProjectRef
    implements VersionedRef<ProjectVersionRef>, Serializable
{

    private static final long serialVersionUID = 1L;

    // NEVER null
    private VersionSpec versionSpec;

    private String versionString;

    public ProjectVersionRef( final ProjectRef ref, final VersionSpec versionSpec )
    {
        this( ref.getGroupId(), ref.getArtifactId(), versionSpec, null );
    }

    public ProjectVersionRef( final ProjectRef ref, final String versionSpec )
        throws InvalidVersionSpecificationException
    {
        this( ref.getGroupId(), ref.getArtifactId(), versionSpec );
    }

    ProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec, final String versionString )
    {
        super( groupId, artifactId );
        if ( versionSpec == null && versionString == null )
        {
            throw new NullPointerException( "Version spec AND string cannot both be null for '" + groupId + ":" + artifactId + "'" );
        }

        this.versionString = versionString;
        this.versionSpec = versionSpec;
    }

    public ProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        this( groupId, artifactId, versionSpec, null );
    }

    public ProjectVersionRef( final String groupId, final String artifactId, final String versionString )
        throws InvalidVersionSpecificationException
    {
        this( groupId, artifactId, null, versionString );
    }

    public static ProjectVersionRef parse( final String gav )
    {
        final String[] parts = gav.split( ":" );
        if ( parts.length < 3 || isEmpty( parts[0] ) || isEmpty( parts[1] ) || isEmpty( parts[2] ) )
        {
            throw new IllegalArgumentException( "ProjectVersionRef must contain non-empty groupId, artifactId, AND version. (Given: '" + gav + "')" );
        }

        return new ProjectVersionRef( parts[0], parts[1], parts[2] );
    }

    public ProjectVersionRef asProjectVersionRef()
    {
        return getClass().equals( ProjectVersionRef.class ) ? this : new ProjectVersionRef( getGroupId(), getArtifactId(), getVersionSpecRaw(),
                                                                                            getVersionStringRaw() );
    }

    public ArtifactRef asPomArtifact()
    {
        return asArtifactRef( "pom", null, false );
    }

    public ArtifactRef asArtifactRef( final String type, final String classifier )
    {
        return asArtifactRef( type, classifier, false );
    }

    public ArtifactRef asArtifactRef( final String type, final String classifier, final boolean optional )
    {
        return new ArtifactRef( this, type, classifier, optional );
    }

    public ArtifactRef asArtifactRef( final TypeAndClassifier tc )
    {
        return asArtifactRef( tc, false );
    }

    public ArtifactRef asArtifactRef( final TypeAndClassifier tc, final boolean optional )
    {
        return new ArtifactRef( this, tc, optional );
    }

    VersionSpec getVersionSpecRaw()
    {
        return versionSpec;
    }

    String getVersionStringRaw()
    {
        return versionString;
    }

    @Override
    public boolean isRelease()
    {
        return getVersionSpec().isRelease();
    }

    @Override
    public boolean isSpecificVersion()
    {
        return getVersionSpec().isSingle();
    }

    @Override
    public boolean matchesVersion( final SingleVersion version )
    {
        return getVersionSpec().contains( version );
    }

    @Override
    public ProjectVersionRef selectVersion( final String version )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single, false );
    }

    @Override
    public ProjectVersionRef selectVersion( final String version, final boolean force )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single, force );
    }

    @Override
    public ProjectVersionRef selectVersion( final SingleVersion version )
    {
        return selectVersion( version, false );
    }

    @Override
    public ProjectVersionRef selectVersion( final SingleVersion version, final boolean force )
    {
        final VersionSpec versionSpec = getVersionSpec();
        if ( versionSpec.equals( version ) )
        {
            return this;
        }

        if ( !force && !versionSpec.contains( version ) )
        {
            throw new IllegalArgumentException( "Specified version: " + version.renderStandard() + " is not contained in spec: "
                + versionSpec.renderStandard() );
        }

        return newRef( getGroupId(), getArtifactId(), version );
    }

    protected ProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new ProjectVersionRef( groupId, artifactId, version );
    }

    @Override
    public synchronized VersionSpec getVersionSpec()
    {
        if ( versionSpec == null )
        {
            versionSpec = VersionUtils.createFromSpec( versionString );
        }
        return versionSpec;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( getVersionString() == null ) ? 0 : getVersionString().hashCode() );
        return result;
    }

    public boolean versionlessEquals( final ProjectVersionRef other )
    {
        if ( this == other )
        {
            return true;
        }

        return super.equals( other );
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ProjectVersionRef other = (ProjectVersionRef) obj;
        boolean result = true;
        try
        {
            if ( getVersionSpec() == null )
            {
                if ( other.getVersionSpec() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionSpec().equals( other.getVersionSpec() ) )
            {
                result = false;
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            if ( getVersionString() == null )
            {
                if ( other.getVersionString() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionString().equals( other.getVersionString() ) )
            {
                result = false;
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getVersionString() );
    }

    @Override
    public boolean isCompound()
    {
        return !getVersionSpec().isSingle();
    }

    @Override
    public boolean isSnapshot()
    {
        return getVersionSpec().isSnapshot();
    }

    @Override
    public synchronized String getVersionString()
    {
        if ( versionString == null )
        {
            versionString = versionSpec.renderStandard();
        }

        return versionString;
    }

    public boolean isVariableVersion()
    {
        return isCompound() || ( isSpecificVersion() && ( (SingleVersion) getVersionSpec() ).isLocalSnapshot() );
    }

    public int compareTo( final ProjectVersionRef other )
    {
        return super.compareTo( other );
    }

}
