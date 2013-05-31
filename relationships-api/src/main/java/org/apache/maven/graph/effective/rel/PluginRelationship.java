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
import java.util.Collection;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;

public final class PluginRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final boolean reporting;

    public PluginRelationship( final URI source, final ProjectVersionRef declaring, final ProjectVersionRef target,
                               final int index, final boolean managed )
    {
        this( source, declaring, target, index, managed, false );
    }

    public PluginRelationship( final URI source, final ProjectVersionRef declaring, final ProjectVersionRef target,
                               final int index, final boolean managed, final boolean reporting )
    {
        super( source, RelationshipType.PLUGIN, declaring, target, index, managed );
        this.reporting = reporting;
    }

    public PluginRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                               final ProjectVersionRef target, final int index, final boolean managed )
    {
        this( source, pomLocation, declaring, target, index, managed, false );
    }

    public PluginRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                               final ProjectVersionRef target, final int index, final boolean managed,
                               final boolean reporting )
    {
        super( source, pomLocation, RelationshipType.PLUGIN, declaring, target, index, managed );
        this.reporting = reporting;
    }

    public PluginRelationship( final Collection<URI> sources, final URI pomLocation, final ProjectVersionRef declaring,
                               final ProjectVersionRef target, final int index, final boolean managed,
                               final boolean reporting )
    {
        super( sources, pomLocation, RelationshipType.PLUGIN, declaring, target, index, managed );
        this.reporting = reporting;
    }

    public final boolean isReporting()
    {
        return reporting;
    }

    @Override
    public synchronized ProjectRelationship<ProjectVersionRef> cloneFor( final ProjectVersionRef projectRef )
    {
        return new PluginRelationship( getSources(), getPomLocation(), projectRef, getTarget(), getIndex(),
                                       isManaged(), reporting );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
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
        final PluginRelationship other = (PluginRelationship) obj;
        if ( isManaged() != other.isManaged() )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "PluginRelationship [%s => %s (managed=%s, index=%s)]", getDeclaring(), getTarget(),
                              isManaged(), getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new ArtifactRef( getTarget(), "maven-plugin", null, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        final ProjectVersionRef d = getDeclaring().selectVersion( version );
        final ProjectVersionRef t = getTarget();

        return new PluginRelationship( getSources(), getPomLocation(), d, t, getIndex(), isManaged(), isReporting() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
    {
        final ProjectVersionRef d = getDeclaring();
        final ProjectVersionRef t = getTarget().selectVersion( version );

        return new PluginRelationship( getSources(), getPomLocation(), d, t, getIndex(), isManaged(), isReporting() );
    }

}
