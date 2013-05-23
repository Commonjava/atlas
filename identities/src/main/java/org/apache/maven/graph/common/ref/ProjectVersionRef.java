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
package org.apache.maven.graph.common.ref;

import java.io.Serializable;

import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.common.version.VersionUtils;

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

    ProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec,
                       final String versionString )
    {
        super( groupId, artifactId );
        if ( versionSpec == null && versionString == null )
        {
            throw new NullPointerException( "Version spec AND string cannot both be null for '" + groupId + ":"
                + artifactId + "'" );
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

    public ProjectVersionRef asProjectVersionRef()
    {
        return getClass().equals( ProjectVersionRef.class ) ? this : new ProjectVersionRef( getGroupId(),
                                                                                            getArtifactId(),
                                                                                            getVersionSpecRaw(),
                                                                                            getVersionStringRaw() );
    }

    VersionSpec getVersionSpecRaw()
    {
        return versionSpec;
    }

    String getVersionStringRaw()
    {
        return versionString;
    }

    public ProjectRef asProjectRef()
    {
        return new ProjectRef( getGroupId(), getArtifactId() );
    }

    public boolean isRelease()
    {
        return getVersionSpec().isRelease();
    }

    public boolean isSpecificVersion()
    {
        return getVersionSpec().isSingle();
    }

    public boolean matchesVersion( final SingleVersion version )
    {
        return getVersionSpec().contains( version );
    }

    public ProjectVersionRef selectVersion( final String version )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single );
    }

    public ProjectVersionRef selectVersion( final SingleVersion version )
    {
        final VersionSpec versionSpec = getVersionSpec();
        if ( versionSpec.equals( version ) )
        {
            return this;
        }

        if ( !versionSpec.contains( version ) )
        {
            throw new IllegalArgumentException( "Specified version: " + version.renderStandard()
                + " is not contained in spec: " + versionSpec.renderStandard() );
        }

        return newRef( getGroupId(), getArtifactId(), version );
    }

    protected ProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new ProjectVersionRef( groupId, artifactId, version );
    }

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

    public boolean isCompound()
    {
        return !getVersionSpec().isSingle();
    }

    public boolean isSnapshot()
    {
        return getVersionSpec().isSnapshot();
    }

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

}
