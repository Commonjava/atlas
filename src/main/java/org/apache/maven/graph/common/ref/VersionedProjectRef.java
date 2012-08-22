package org.apache.maven.graph.common.ref;

import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.common.version.VersionUtils;

public class VersionedProjectRef
    extends ProjectRef
    implements VersionedRef<VersionedProjectRef>
{

    private final VersionSpec versionSpec;

    public VersionedProjectRef( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        super( groupId, artifactId );
        this.versionSpec = versionSpec;
    }

    public VersionedProjectRef( final String groupId, final String artifactId, final String versionSpec )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId );
        this.versionSpec = VersionUtils.createFromSpec( versionSpec );
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

    public VersionedProjectRef selectVersion( final SingleVersion version )
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

        return newRef( getGroupId(), getArtifactId(), version );
    }

    protected VersionedProjectRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new VersionedProjectRef( groupId, artifactId, version );
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

    public boolean versionlessEquals( final VersionedProjectRef other )
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
        final VersionedProjectRef other = (VersionedProjectRef) obj;
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
