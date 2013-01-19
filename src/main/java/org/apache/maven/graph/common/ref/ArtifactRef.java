/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.common.ref;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.Serializable;

import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionSpec;

public class ArtifactRef
    extends ProjectVersionRef
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final String type;

    private final String classifier;

    private final boolean optional;

    public ArtifactRef( final String groupId, final String artifactId, final VersionSpec version, final String type,
                        final String classifier, final boolean optional )
    {
        super( groupId, artifactId, version );
        this.optional = optional;
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
    }

    public ArtifactRef( final ProjectVersionRef ref, final String type, final String classifier, final boolean optional )
    {
        super( ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpec() );
        this.optional = optional;
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
    }

    public ArtifactRef( final String groupId, final String artifactId, final String versionSpec, final String type,
                        final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId, versionSpec );
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
        this.optional = optional;
    }

    @Override
    protected ProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new ArtifactRef( groupId, artifactId, version, type, classifier, optional );
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean isOptional()
    {
        return optional;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( classifier == null ) ? 0 : classifier.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        result = prime * result + Boolean.valueOf( optional )
                                         .hashCode();
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
        final ArtifactRef other = (ArtifactRef) obj;

        return artifactFieldsEqual( other );
    }

    private boolean artifactFieldsEqual( final ArtifactRef other )
    {
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

    @Override
    public boolean versionlessEquals( final ProjectVersionRef other )
    {
        if ( !super.versionlessEquals( other ) )
        {
            return false;
        }

        if ( !( other instanceof ArtifactRef ) )
        {
            // compare vs. POM reference.
            return artifactFieldsEqual( new ArtifactRef( other, "pom", null, false ) );
        }

        return artifactFieldsEqual( (ArtifactRef) other );
    }

}
