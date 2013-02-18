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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class DependencyRelationship
    extends AbstractProjectRelationship<ArtifactRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final DependencyScope scope;

    private final Set<ProjectRef> excludes;

    public DependencyRelationship( final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed,
                                   final ProjectRef... excludes )
    {
        super( RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public final DependencyScope getScope()
    {
        return scope;
    }

    @Override
    public synchronized ProjectRelationship<ArtifactRef> cloneFor( final ProjectVersionRef projectRef )
    {
        return new DependencyRelationship( projectRef, getTarget(), scope, getIndex(), isManaged() );
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
        final DependencyRelationship other = (DependencyRelationship) obj;
        if ( isManaged() != other.isManaged() )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "DependencyRelationship [%s => %s (managed=%s, scope=%s, index=%s)]", getDeclaring(),
                              getTarget(), isManaged(), scope, getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

    public Set<ProjectRef> getExcludes()
    {
        return excludes;
    }

}
