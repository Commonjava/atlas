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

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.Serializable;

public class ProjectRef
    implements Serializable, Comparable<ProjectRef>
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

    public static ProjectRef parse( final String ga )
    {
        final String[] parts = ga.split( ":" );
        if ( parts.length < 2 || isEmpty( parts[0] ) || isEmpty( parts[1] ) )
        {
            throw new IllegalArgumentException( "ProjectRef must contain non-empty groupId AND artifactId. (Given: '"
                + ga + "')" );
        }

        return new ProjectRef( parts[0], parts[1] );
    }

    public final String getGroupId()
    {
        return groupId;
    }

    public final String getArtifactId()
    {
        return artifactId;
    }

    public ProjectRef asProjectRef()
    {
        return new ProjectRef( getGroupId(), getArtifactId() );
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s", groupId, artifactId );
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

    @Override
    public int compareTo( final ProjectRef o )
    {
        int comp = groupId.compareTo( o.groupId );
        if ( comp == 0 )
        {
            comp = artifactId.compareTo( o.artifactId );
        }

        return 0;
    }

}
