package org.apache.maven.graph.common.ref;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.Serializable;

public class ProjectRef
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    // NEVER null
    private final String groupId;

    // NEVER null
    private final String artifactId;

    public ProjectRef( final String groupId, final String artifactId )
    {
        if ( isEmpty( groupId ) || isEmpty( artifactId ) )
        {
            throw new IllegalArgumentException( "ProjectId must contain non-empty groupId AND artifactId. (Given: '"
                + groupId + "':'" + artifactId + "')" );
        }

        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public final String getGroupId()
    {
        return groupId;
    }

    public final String getArtifactId()
    {
        return artifactId;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:*", groupId, artifactId );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + artifactId.hashCode();
        result = prime * result + groupId.hashCode();
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ProjectRef other = (ProjectRef) obj;
        if ( !artifactId.equals( other.artifactId ) )
        {
            return false;
        }
        if ( !groupId.equals( other.groupId ) )
        {
            return false;
        }
        return true;
    }

}
