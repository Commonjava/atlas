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

import java.io.Serializable;

import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;

public class ArtifactRef
    extends ProjectVersionRef
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final TypeAndClassifier tc;

    private final boolean optional;

    public ArtifactRef( final String groupId, final String artifactId, final VersionSpec version, final String type,
                        final String classifier, final boolean optional )
    {
        super( groupId, artifactId, version );
        this.optional = optional;
        this.tc = new TypeAndClassifier( type, classifier );
    }

    public ArtifactRef( final ProjectVersionRef ref, final String type, final String classifier, final boolean optional )
    {
        super( ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpecRaw(), ref.getVersionStringRaw() );
        this.optional = optional;
        this.tc = new TypeAndClassifier( type, classifier );
    }

    public ArtifactRef( final ProjectVersionRef ref, final TypeAndClassifier tc, final boolean optional )
    {
        super( ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpecRaw(), ref.getVersionStringRaw() );
        this.tc = tc;
        this.optional = optional;
    }

    public ArtifactRef( final String groupId, final String artifactId, final String versionSpec, final String type,
                        final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId, versionSpec );
        this.tc = new TypeAndClassifier( type, classifier );
        this.optional = optional;
    }

    @Override
    protected ProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new ArtifactRef( groupId, artifactId, version, tc.getType(), tc.getClassifier(), optional );
    }

    public String getType()
    {
        return tc.getType();
    }

    public String getClassifier()
    {
        return tc.getClassifier();
    }

    public TypeAndClassifier getTypeAndClassifier()
    {
        return tc;
    }

    public ArtifactRef setOptional( final boolean optional )
    {
        if ( this.optional == optional )
        {
            return this;
        }

        return new ArtifactRef( this, getType(), getClassifier(), optional );
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
        result = prime * result + ( ( tc == null ) ? 0 : tc.hashCode() );
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
        if ( optional != other.optional )
        {
            return false;
        }

        if ( tc == null )
        {
            if ( other.tc != null )
            {
                return false;
            }
        }
        else if ( !tc.equals( other.tc ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s:%s", getGroupId(), getArtifactId(), getVersionString(), getTypeAndClassifier() );
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
