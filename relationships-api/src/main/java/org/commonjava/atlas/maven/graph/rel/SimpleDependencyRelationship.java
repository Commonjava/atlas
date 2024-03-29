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
package org.commonjava.atlas.maven.graph.rel;

import org.commonjava.atlas.maven.ident.DependencyScope;
import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class SimpleDependencyRelationship
    extends AbstractSimpleProjectRelationship<DependencyRelationship, ArtifactRef>
    implements Serializable, DependencyRelationship
{

    private static final long serialVersionUID = 1L;

    private final DependencyScope scope;

    private final Set<ProjectRef> excludes;

    private boolean optional;

    public SimpleDependencyRelationship( final URI source, final ProjectVersionRef declaring, final ArtifactRef target,
                                         final DependencyScope scope, final int index, final boolean managed,
                                         final boolean inherited, final boolean optional, final ProjectRef... excludes )
    {
        super( source, RelationshipType.DEPENDENCY, declaring, target, index, managed, inherited, false );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.optional = optional;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public SimpleDependencyRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                                         final ArtifactRef target, final DependencyScope scope, final int index,
                                         final boolean managed, final boolean inherited, final boolean optional,
                                         final ProjectRef... excludes )
    {
        super( source, pomLocation, RelationshipType.DEPENDENCY, declaring, target, index, managed, inherited, false );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.optional = optional;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public SimpleDependencyRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                                         final ArtifactRef target, final DependencyScope scope, final int index,
                                         final boolean managed, final boolean inherited, final boolean optional,
                                         final ProjectRef... excludes )
    {
        super( sources, RelationshipType.DEPENDENCY, declaring, target, index, managed, inherited, false );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.optional = optional;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public SimpleDependencyRelationship( final Collection<URI> sources, final URI pomLocation,
                                         final ProjectVersionRef declaring, final ArtifactRef target,
                                         final DependencyScope scope, final int index, final boolean managed,
                                         final boolean inherited, final boolean optional, final ProjectRef... excludes )
    {
        super( sources, pomLocation, RelationshipType.DEPENDENCY, declaring, target, index, managed, inherited, false );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.optional = optional;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public SimpleDependencyRelationship( final DependencyRelationship relationship )
    {
        super( relationship );
        this.scope = relationship.getScope();
        this.optional = relationship.isOptional();
        this.excludes = new HashSet<ProjectRef>( relationship.getExcludes() );
    }

    @Override
    public final DependencyScope getScope()
    {
        return scope;
    }

    @Override
    public boolean isOptional()
    {
        return optional;
    }

    @Override
    public synchronized DependencyRelationship cloneFor( final ProjectVersionRef projectRef )
    {
        return new SimpleDependencyRelationship( getSources(), getPomLocation(), projectRef, getTarget(), scope, getIndex(),
                                           isManaged(), isInherited(), optional );
    }

    @Override
    public DependencyRelationship addSource( final URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimpleDependencyRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), scope, getIndex(),
                                                 isManaged(), isInherited(), optional );
    }

    @Override
    public DependencyRelationship addSources( final Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimpleDependencyRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), scope, getIndex(),
                                                 isManaged(), isInherited(), optional );
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
        if ( !( obj instanceof DependencyRelationship ) )
        {
            return false;
        }
        final DependencyRelationship other = (DependencyRelationship) obj;
        return isManaged() == other.isManaged();
    }

    @Override
    public String toString()
    {
        return String.format( "DependencyRelationship [%s => %s (managed=%s, scope=%s, index=%s)]", getDeclaring(),
                              getTarget(), isManaged(), scope, getIndex() );
    }

    @Override
    protected ArtifactRef cloneTarget( final ArtifactRef target )
    {
        return new SimpleArtifactRef( target );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

    @Override
    public Set<ProjectRef> getExcludes()
    {
        return excludes;
    }

    @Override
    public DependencyRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ArtifactRef t = getTarget();

        Set<ProjectRef> var = getExcludes();
        return new SimpleDependencyRelationship( getSources(), getPomLocation(), ref, t, getScope(), getIndex(), isManaged(),
                                                 isInherited(), optional, var.toArray( new ProjectRef[var.size()] ) );
    }

    @Override
    public DependencyRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        ArtifactRef t = getTarget();
        t =
            (ArtifactRef) ( ( ref instanceof ArtifactRef ) ? ref : new SimpleArtifactRef( ref, t.getType(),
                                                                                    t.getClassifier() ) );

        Set<ProjectRef> var = getExcludes();
        return new SimpleDependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                                 isInherited(), optional, var.toArray( new ProjectRef[var.size()] ) );
    }

    @Override
    public boolean isBOM()
    {
        return DependencyScope._import == getScope() && "pom".equals( getTargetArtifact().getType() );
    }

}
