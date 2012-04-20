package org.apache.maven.pgraph.id;

import org.apache.maven.pgraph.version.SingleVersion;
import org.apache.maven.pgraph.version.VersionSpec;

public class ProjectVersion
    implements Versioned<ProjectVersion>
{
    
    private final ProjectId projectId;

    private final VersionSpec versionSpec;
    
    public ProjectVersion( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        projectId = new ProjectId( groupId, artifactId );
        this.versionSpec = versionSpec;
    }
    
    public final ProjectId getProjectId()
    {
        return projectId;
    }

    public boolean isRelease()
    {
        return ( versionSpec instanceof SingleVersion ) && ( (SingleVersion) versionSpec ).isRelease();
    }

    public boolean isSpecificVersion()
    {
        return ( versionSpec instanceof SingleVersion );
    }

    public final String getGroupId()
    {
        return projectId.getGroupId();
    }

    public final String getArtifactId()
    {
        return projectId.getArtifactId();
    }

    public boolean matchesVersion( final SingleVersion version )
    {
        return versionSpec.contains( version );
    }

    public ProjectVersion selectVersion( final SingleVersion version )
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

        return new ProjectVersion( getGroupId(), getArtifactId(), version );
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
        final ProjectVersion other = (ProjectVersion) obj;
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
        return !( versionSpec instanceof SingleVersion );
    }

    public boolean isSnapshot()
    {
        return !isCompound() && !isRelease();
    }

}
