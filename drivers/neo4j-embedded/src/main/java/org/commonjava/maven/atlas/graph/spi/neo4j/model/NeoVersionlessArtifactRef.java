/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleVersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Special implementation of {@link NeoArtifactRef} that forces all versions to ZERO, to allow calculation of transitive
 * dependency graphs, where version collisions of the same project are likely.
 *
 * @author jdcasey
 */
public class NeoVersionlessArtifactRef<T extends VersionlessArtifactRef>
        extends NeoProjectRef<T>
        implements VersionlessArtifactRef
{

    private static final long serialVersionUID = 1L;

    private TypeAndClassifier tc;

    public NeoVersionlessArtifactRef( final ArtifactRef ref )
    {
        super( ref.getGroupId(), ref.getArtifactId() );
        this.tc = ref.getTypeAndClassifier();
    }

    public NeoVersionlessArtifactRef( final ProjectRef ref, final String type, final String classifier )
    {
        super( ref.getGroupId(), ref.getArtifactId() );
        this.tc = new NeoTypeAndClassifier( type, classifier );
    }

    public NeoVersionlessArtifactRef( final ProjectRef ref, final TypeAndClassifier tc )
    {
        super( ref.getGroupId(), ref.getArtifactId() );
        this.tc = tc == null ? new NeoTypeAndClassifier() : tc;
    }

    public NeoVersionlessArtifactRef( final String groupId, final String artifactId, final String type,
                                      final String classifier, final boolean optional )
            throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId );
        this.tc = new NeoTypeAndClassifier( type, classifier );
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getTypeAndClassifier() );
    }

    public static VersionlessArtifactRef parse( final String spec )
    {
        final String[] parts = spec.split( ":" );

        if ( parts.length < 2 || isEmpty( parts[0] ) || isEmpty( parts[1] ) )
        {
            throw new InvalidRefException(
                    "VersionlessArtifactRef must contain AT LEAST non-empty groupId and artifactId. (Given: '" + spec
                            + "')" );
        }

        final String g = parts[0];
        final String a = parts[1];

        String t = "pom";
        String c = null;

        if ( parts.length > 2 )
        {
            // we probably have a type in there.
            t = parts[2];

            if ( parts.length > 3 )
            {
                // we have a classifier? What if it's GATV??
                // assume it's just a classifier...
                c = parts[3];

                if ( parts.length > 4 )
                {
                    // okay, wtf? It's a GATVC, and we need to shift to eliminate the V...
                    c = parts[4];
                }
            }
        }

        // assume non-optional, because it might not matter if you're parsing a string like this...you'd be more careful if you were reading something
        // that had an optional field, because it's not in the normal GATV[C] spec.
        return new NeoVersionlessArtifactRef( g, a, t, c, false );
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
        if ( !( obj instanceof VersionlessArtifactRef ) )
        {
            return false;
        }
        final VersionlessArtifactRef other = (VersionlessArtifactRef) obj;
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
    public VersionlessArtifactRef asVersionlessPomArtifact()
    {
        return asVersionlessArtifactRef( "pom", null );
    }

    @Override
    public VersionlessArtifactRef asVersionlessJarArtifact()
    {
        return asVersionlessArtifactRef( "jar", null );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final String type, final String classifier )
    {
        final TypeAndClassifier tc = new NeoTypeAndClassifier( type, classifier );
        if ( NeoVersionlessArtifactRef.class.equals( getClass() ) && this.tc.equals( tc ) )
        {
            return this;
        }

        return super.asVersionlessArtifactRef( type, classifier );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc )
    {
        if ( NeoVersionlessArtifactRef.class.equals( getClass() ) && this.tc.equals( tc ) )
        {
            return this;
        }

        return super.asVersionlessArtifactRef( tc );
    }

    @Override
    public T detach()
    {
        return (T) new SimpleVersionlessArtifactRef( this );
    }
}
