/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.rel;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

public final class ParentRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    /**
     * Ancestry terminus. This is to signify that the declaring project has NO parent relationship.
     */
    public ParentRelationship( final URI source, final ProjectVersionRef declaring )
    {
        super( source, RelationshipType.PARENT, declaring, declaring, 0 );
    }

    public ParentRelationship( final URI source, final ProjectVersionRef declaring, final ProjectVersionRef target )
    {
        super( source, RelationshipType.PARENT, declaring, target, 0 );
    }

    public ParentRelationship( final Collection<URI> sources, final ProjectVersionRef declaring, final ProjectVersionRef target )
    {
        super( sources, RelationshipType.PARENT, declaring, target, 0 );
    }

    @Override
    public String toString()
    {
        return String.format( "ParentRelationship [%s => %s]", getDeclaring(), getTarget() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new ArtifactRef( getTarget(), "pom", null, false );
    }

    public boolean isTerminus()
    {
        return getDeclaring().equals( getTarget() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        ProjectVersionRef d = getDeclaring();
        final ProjectVersionRef t = getTarget();
        final boolean self = d.equals( t );

        d = d.selectVersion( version, force );

        return new ParentRelationship( getSources(), d, self ? d : t );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
    {
        return selectTarget( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring();
        ProjectVersionRef t = getTarget();
        final boolean self = d.equals( t );

        t = t.selectVersion( version, force );

        return new ParentRelationship( getSources(), self ? t : d, t );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new ParentRelationship( getSources(), ref, t );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        return new ParentRelationship( getSources(), d, ref );
    }

}
