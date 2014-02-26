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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

public final class DependencyRelationship
    extends AbstractProjectRelationship<ArtifactRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final DependencyScope scope;

    private final Set<ProjectRef> excludes;

    public DependencyRelationship( final URI source, final ProjectVersionRef declaring, final ArtifactRef target, final DependencyScope scope,
                                   final int index, final boolean managed, final ProjectRef... excludes )
    {
        super( source, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public DependencyRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed, final ProjectRef... excludes )
    {
        super( source, pomLocation, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public DependencyRelationship( final Collection<URI> sources, final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed, final ProjectRef... excludes )
    {
        super( sources, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public DependencyRelationship( final Collection<URI> sources, final URI pomLocation, final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed, final ProjectRef... excludes )
    {
        super( sources, pomLocation, RelationshipType.DEPENDENCY, declaring, target, index, managed );
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
        return new DependencyRelationship( getSources(), getPomLocation(), projectRef, getTarget(), scope, getIndex(), isManaged() );
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
        return String.format( "DependencyRelationship [%s => %s (managed=%s, scope=%s, index=%s)]", getDeclaring(), getTarget(), isManaged(), scope,
                              getIndex() );
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

    @Override
    public ProjectRelationship<ArtifactRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring().selectVersion( version, force );
        final ArtifactRef t = getTarget();

        return new DependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                           getExcludes().toArray( new ProjectRef[] {} ) );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectTarget( final SingleVersion version )
    {
        return selectTarget( version, false );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectTarget( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring();
        ArtifactRef t = getTarget();
        t = new ArtifactRef( t.selectVersion( version, force ), t.getType(), t.getClassifier(), t.isOptional() );

        return new DependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                           getExcludes().toArray( new ProjectRef[] {} ) );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectDeclaring( final ProjectVersionRef ref )
    {
        final ArtifactRef t = getTarget();

        return new DependencyRelationship( getSources(), getPomLocation(), ref, t, getScope(), getIndex(), isManaged(),
                                           getExcludes().toArray( new ProjectRef[] {} ) );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        ArtifactRef t = getTarget();
        t = (ArtifactRef) ( ( ref instanceof ArtifactRef ) ? ref : new ArtifactRef( ref, t.getType(), t.getClassifier(), t.isOptional() ) );

        return new DependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                           getExcludes().toArray( new ProjectRef[] {} ) );
    }

    public boolean isBOM()
    {
        return DependencyScope._import == getScope() && "pom".equals( getTargetArtifact().getType() );
    }

}
