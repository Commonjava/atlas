package org.apache.maven.pgraph.id;

import static org.apache.commons.lang.StringUtils.isEmpty;

import org.apache.maven.pgraph.version.SingleVersion;
import org.apache.maven.pgraph.version.VersionSpec;

public class Artifact
    implements Versioned<Artifact>
{

    private final ProjectVersion projectVersion;

    private final String type;

    private final String classifier;

    public Artifact( final String groupId, final String artifactId, final VersionSpec version, final String type,
                     final String classifier )
    {
        this( new ProjectVersion( groupId, artifactId, version ), type, classifier );
    }

    public Artifact( final ProjectVersion projectVersion, final String type, final String classifier )
    {
        this.projectVersion = projectVersion;
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
    }

    public ProjectVersion getProjectVersion()
    {
        return projectVersion;
    }

    public final ProjectId getProjectId()
    {
        return projectVersion.getProjectId();
    }

    public boolean isSpecificVersion()
    {
        return projectVersion.isSpecificVersion();
    }

    public boolean isRelease()
    {
        return projectVersion.isRelease();
    }

    public final String getGroupId()
    {
        return projectVersion.getGroupId();
    }

    public final String getArtifactId()
    {
        return projectVersion.getArtifactId();
    }

    public boolean matchesVersion( final SingleVersion version )
    {
        return projectVersion.matchesVersion( version );
    }

    public Artifact selectVersion( final SingleVersion version )
    {
        return new Artifact( projectVersion.selectVersion( version ), type, classifier );
    }

    public VersionSpec getVersionSpec()
    {
        return projectVersion.getVersionSpec();
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( classifier == null ) ? 0 : classifier.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
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
        final Artifact other = (Artifact) obj;
        if ( classifier == null )
        {
            if ( other.classifier != null )
            {
                return false;
            }
        }
        else if ( !classifier.equals( other.classifier ) )
        {
            return false;
        }
        if ( type == null )
        {
            if ( other.type != null )
            {
                return false;
            }
        }
        else if ( !type.equals( other.type ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        if ( classifier != null )
        {
            return String.format( "%s:%s:%s:%s:%s", getGroupId(), getArtifactId(), getVersionSpec().renderStandard(),
                                  getType(), getClassifier() );
        }
        else
        {
            return String.format( "%s:%s:%s:%s", getGroupId(), getArtifactId(), getVersionSpec().renderStandard(),
                                  getType() );
        }
    }

    public boolean isSnapshot()
    {
        return projectVersion.isSnapshot();
    }

    public boolean isCompound()
    {
        return projectVersion.isCompound();
    }

}
