package org.apache.maven.graph.common.ref;

import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.common.version.VersionUtils;

public class ProjectVersionRef
    extends ProjectRef
    implements VersionedRef<ProjectVersionRef>
{

    // NEVER null
    private final VersionSpec versionSpec;

    public ProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        super( groupId, artifactId );
        if ( versionSpec == null )
        {
            throw new IllegalArgumentException( "Version cannot be null for '" + groupId + ":" + artifactId + "'" );
        }

        this.versionSpec = versionSpec;
    }

    public ProjectVersionRef( final String groupId, final String artifactId, final String versionSpec )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId );
        if ( versionSpec == null || versionSpec.trim()
                                               .length() < 1 )
        {
            throw new IllegalArgumentException( "Version ('" + versionSpec + "') cannot be null or empty for '"
                + groupId + ":" + artifactId + "'" );
        }

        this.versionSpec = VersionUtils.createFromSpec( versionSpec );
    }

    public boolean isRelease()
    {
        return versionSpec.isConcrete();
    }

    public boolean isSpecificVersion()
    {
        return versionSpec.isSingle();
    }

    public boolean matchesVersion( final SingleVersion version )
    {
        return versionSpec.contains( version );
    }

    public ProjectVersionRef selectVersion( final SingleVersion version )
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

    protected ProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new ProjectVersionRef( groupId, artifactId, version );
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
        return String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getVersionSpec().renderStandard() );
    }

    public boolean isCompound()
    {
        return !versionSpec.isSingle();
    }

    public boolean isSnapshot()
    {
        return !isCompound() && !isRelease();
    }

}
