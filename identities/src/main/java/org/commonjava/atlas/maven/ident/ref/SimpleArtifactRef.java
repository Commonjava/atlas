/**
 * Copyright (C) 2012-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.ident.ref;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;

import org.commonjava.atlas.maven.ident.version.InvalidVersionSpecificationException;
import org.commonjava.atlas.maven.ident.version.SingleVersion;
import org.commonjava.atlas.maven.ident.version.VersionSpec;

/**
 * Represents an artifact, in Maven parlance. A tangible result of a build, which is typically made available in a Maven repository for others
 * to consume. NOTE: a POM file is both a piece of metadata AND an artifact in the Maven world!
 *
 * Artifacts are based on the GAV coordinate for the project release, but also contain a type and optionally, a classifier. Type defaults to 'jar'.
 *
 * @see {@link SimpleProjectRef}
 * @see {@link SimpleProjectVersionRef}
 *
 * @author jdcasey
 */
public class SimpleArtifactRef
    extends SimpleProjectVersionRef
    implements Serializable, ArtifactRef
{

    private static final long serialVersionUID = 1L;

    private final TypeAndClassifier tc;

    public SimpleArtifactRef( final String groupId, final String artifactId, final VersionSpec version,
                              final String type, final String classifier )
    {
        super( groupId, artifactId, version );
        this.tc = new SimpleTypeAndClassifier( type, classifier );
    }

    public SimpleArtifactRef( final ProjectVersionRef ref, final String type, final String classifier )
    {
        super( ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpecRaw(), ref.getVersionStringRaw() );
        this.tc = new SimpleTypeAndClassifier( type, classifier );
    }

    public SimpleArtifactRef( final ProjectVersionRef ref, final TypeAndClassifier tc )
    {
        super( ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpecRaw(), ref.getVersionStringRaw() );
        this.tc = getSimpleTypeAndClassifier( tc );
    }

    public SimpleArtifactRef( final String groupId, final String artifactId, final String versionSpec,
                              final String type, final String classifier )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId, versionSpec );
        this.tc = new SimpleTypeAndClassifier( type, classifier );
    }

    public <T extends ArtifactRef> SimpleArtifactRef( final ArtifactRef ref )
    {
        super( ref );
        this.tc = getSimpleTypeAndClassifier( ref.getTypeAndClassifier() );
    }

    private SimpleTypeAndClassifier getSimpleTypeAndClassifier( final TypeAndClassifier tc )
    {
        if ( tc instanceof SimpleTypeAndClassifier )
        {
            return ( SimpleTypeAndClassifier ) tc;
        }
        else
        {
            return new SimpleTypeAndClassifier( tc.getType(), tc.getClassifier() );
        }
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s:%s%s", getGroupId(), getArtifactId(), getType(), getVersionString(),
                              ( getClassifier() == null ? "" : ":" + getClassifier() ) );
    }

    public static SimpleArtifactRef parse( final String spec )
    {
        final String[] parts = spec.split( ":" );

        if ( parts.length < 3 || isEmpty( parts[0] ) || isEmpty( parts[1] ) || isEmpty( parts[2] ) )
        {
            throw new InvalidRefException(
                                           "SimpleArtifactRef must contain AT LEAST non-empty groupId, artifactId, AND version. (Given: '"
                                               + spec + "')" );
        }

        final String g = parts[0];
        final String a = parts[1];

        // assume we're actually parsing a GAV into a POM artifact...
        String v = parts[2];
        String t = "pom";
        String c = null;

        if ( parts.length > 3 )
        {
            // oops, it's a type, not a version...see toString() for the specification.
            t = v;
            v = parts[3];

            if ( parts.length > 4 )
            {
                c = parts[4];
            }
        }

        // assume non-optional, because it might not matter if you're parsing a string like this...you'd be more careful if you were reading something
        // that had an optional field, because it's not in the normal GATV[C] spec.
        return new SimpleArtifactRef( g, a, v, t, c );
    }

    @Override
    public SimpleArtifactRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new SimpleArtifactRef( groupId, artifactId, version, tc.getType(), tc.getClassifier() );
    }

    @Override
    public String getType()
    {
        return tc.getType();
    }

    @Override
    public String getClassifier()
    {
        return tc.getClassifier();
    }

    @Override
    public TypeAndClassifier getTypeAndClassifier()
    {
        return tc;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( tc == null ) ? 0 : tc.hashCode() );
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
        if ( !(obj instanceof ArtifactRef) )
        {
            return false;
        }
        final ArtifactRef other = (ArtifactRef) obj;

        return artifactFieldsEqual( other );
    }

    private boolean artifactFieldsEqual( final ArtifactRef other )
    {
        if ( tc == null )
        {
            if ( other.getTypeAndClassifier() != null )
            {
                return false;
            }
        }
        else if ( !tc.equals( other.getTypeAndClassifier() ) )
        {
            return false;
        }
        return true;
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
            return artifactFieldsEqual( new SimpleArtifactRef( other, "pom", null ) );
        }

        return artifactFieldsEqual( (ArtifactRef) other );
    }

}
