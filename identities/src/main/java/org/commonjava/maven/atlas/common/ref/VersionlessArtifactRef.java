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
package org.commonjava.maven.atlas.common.ref;

import org.commonjava.maven.atlas.common.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.common.version.SingleVersion;
import org.commonjava.maven.atlas.common.version.part.NumericPart;

/**
 * Special implementation of {@link ArtifactRef} that forces all versions to ZERO, to allow calculation of transitive
 * dependency graphs, where version collisions of the same project are likely.
 * 
 * @author jdcasey
 */
public class VersionlessArtifactRef
    extends ArtifactRef
{

    private static final long serialVersionUID = 1L;

    private static SingleVersion DUMMY_VERSION;

    static
    {
        try
        {
            DUMMY_VERSION = new SingleVersion( "1", new NumericPart( 1 ) );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            // TODO: What do I do with this? It should NEVER happen.
        }
    }

    private ArtifactRef realRef;

    public VersionlessArtifactRef( final ArtifactRef ref )
    {
        super( new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), DUMMY_VERSION ), ref.getType(),
               ref.getClassifier(), ref.isOptional() );

        this.realRef = ref;
    }

    public void replaceRealRef( final ArtifactRef ref )
    {
        if ( realRef.versionlessEquals( ref ) )
        {
            this.realRef = ref;
        }
    }

    public ArtifactRef getRealRef()
    {
        return realRef;
    }

    /**
     * Leave out the version!
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getArtifactId() == null ) ? 0 : getArtifactId().hashCode() );
        result = prime * result + ( ( getGroupId() == null ) ? 0 : getGroupId().hashCode() );
        result = prime * result + ( ( getClassifier() == null ) ? 0 : getClassifier().hashCode() );
        result = prime * result + ( ( getType() == null ) ? 0 : getType().hashCode() );
        result = prime * result + Boolean.valueOf( isOptional() )
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

        final ArtifactRef other = (ArtifactRef) obj;
        return versionlessEquals( other );
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:*:%s%s", getGroupId(), getArtifactId(), getType(), ( getClassifier() == null ? ""
                        : ":" + getClassifier() ) );
    }
}
