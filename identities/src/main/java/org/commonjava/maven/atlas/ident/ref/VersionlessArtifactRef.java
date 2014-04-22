/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.ident.ref;

import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

/**
 * Special implementation of {@link ArtifactRef} that forces all versions to ZERO, to allow calculation of transitive
 * dependency graphs, where version collisions of the same project are likely.
 * 
 * @author jdcasey
 */
public class VersionlessArtifactRef
    extends ProjectRef
{

    private static final long serialVersionUID = 1L;

    private final TypeAndClassifier tc;

    private final boolean optional;

    public VersionlessArtifactRef( final ArtifactRef ref )
    {
        super( ref.getGroupId(), ref.getArtifactId() );
        this.optional = ref.isOptional();
        this.tc = ref.getTypeAndClassifier();
    }

    public VersionlessArtifactRef( final ProjectRef ref, final String type, final String classifier, final boolean optional )
    {
        super( ref.getGroupId(), ref.getArtifactId() );
        this.optional = optional;
        this.tc = new TypeAndClassifier( type, classifier );
    }

    public VersionlessArtifactRef( final ProjectRef ref, final TypeAndClassifier tc, final boolean optional )
    {
        super( ref.getGroupId(), ref.getArtifactId() );
        this.tc = tc == null ? new TypeAndClassifier() : tc;
        this.optional = optional;
    }

    public VersionlessArtifactRef( final String groupId, final String artifactId, final String type, final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId );
        this.tc = new TypeAndClassifier( type, classifier );
        this.optional = optional;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getTypeAndClassifier() );
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

    public boolean isOptional()
    {
        return optional;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( optional ? 1231 : 1237 );
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
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final VersionlessArtifactRef other = (VersionlessArtifactRef) obj;
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
    public VersionlessArtifactRef asVersionlessPomArtifact()
    {
        return asVersionlessArtifactRef( "pom", null, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessJarArtifact()
    {
        return asVersionlessArtifactRef( "jar", null, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final String type, final String classifier )
    {
        return asVersionlessArtifactRef( type, classifier, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final String type, final String classifier, final boolean optional )
    {
        final TypeAndClassifier tc = new TypeAndClassifier( type, classifier );
        if ( VersionlessArtifactRef.class.equals( getClass() ) && this.tc.equals( tc ) && this.optional == optional )
        {
            return this;
        }

        return super.asVersionlessArtifactRef( type, classifier, optional );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc )
    {
        return asVersionlessArtifactRef( tc, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc, final boolean optional )
    {
        if ( VersionlessArtifactRef.class.equals( getClass() ) && this.tc.equals( tc ) && this.optional == optional )
        {
            return this;
        }

        return super.asVersionlessArtifactRef( tc, optional );
    }
}
