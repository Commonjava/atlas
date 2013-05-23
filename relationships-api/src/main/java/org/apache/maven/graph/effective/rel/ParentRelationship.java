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
package org.apache.maven.graph.effective.rel;

import java.io.Serializable;
import java.net.URI;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;

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

    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        ProjectVersionRef d = getDeclaring();
        final ProjectVersionRef t = getTarget();
        final boolean self = d.equals( t );

        d = d.selectVersion( version );

        return new ParentRelationship( getSource(), d, self ? d : t );
    }

    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
    {
        final ProjectVersionRef d = getDeclaring();
        ProjectVersionRef t = getTarget();
        final boolean self = d.equals( t );

        t = t.selectVersion( version );

        return new ParentRelationship( getSource(), self ? t : d, t );
    }

}
