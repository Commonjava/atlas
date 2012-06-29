package org.apache.maven.pgraph.id;

import org.apache.maven.pgraph.version.SingleVersion;
import org.apache.maven.pgraph.version.VersionSpec;

public class VersionedProjectId
    extends ProjectId
    implements Versioned<VersionedProjectId>
{

    private final VersionSpec versionSpec;

    public VersionedProjectId( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        super( groupId, artifactId );
        this.versionSpec = versionSpec;
    }

    public boolean isRelease()
    {
        return versionSpec != null && versionSpec.isConcrete();
    }

    public boolean isSpecificVersion()
    {
        return versionSpec != null && versionSpec.isSingle();
    }

    public boolean matchesVersion( final SingleVersion version )
    {
        return versionSpec.contains( version );
    }

    public VersionedProjectId selectVersion( final SingleVersion version )
    {
        if ( versionSpec.equals( version ) )
        {
            return this;
        }

        if ( !versionSpec.contains( version ) )
        {
            throw new IllegalArgumentException( "Specified version: " + version.renderStandard()
                + " is not contained in spec: " + versionSpec.renderStandard() );
        }

        return new VersionedProjectId( getGroupId(), getArtifactId(), version );
    }

    public VersionSpec getVersionSpec()
    {
        return versionSpec;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( getVersionSpec() == null ) ? 0 : getVersionSpec().hashCode() );
        return result;
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
        final VersionedProjectId other = (VersionedProjectId) obj;
        if ( getVersionSpec() == null )
        {
            if ( other.getVersionSpec() != null )
            {
                return false;
            }
        }
        else if ( !getVersionSpec().equals( other.getVersionSpec() ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getVersionSpec() == null ? "-NONE-"
                        : getVersionSpec().renderStandard() );
    }

    public boolean isCompound()
    {
        return versionSpec != null && !versionSpec.isSingle();
    }

    public boolean isSnapshot()
    {
        return !isCompound() && !isRelease();
    }

}
