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
package org.commonjava.maven.atlas.graph.filter;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public class DependencyFilter
    extends AbstractTypedFilter
{

    private final DependencyScope scope;

    private final ScopeTransitivity scopeTransitivity;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    private final boolean useImpliedScopes;

    public DependencyFilter()
    {
        this( DependencyScope.test, ScopeTransitivity.maven, false, true, true, null );
    }

    public DependencyFilter( final DependencyScope scope )
    {
        this( scope, ScopeTransitivity.maven, false, true, true, null );
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity, final boolean includeManaged,
                             final boolean includeConcrete, final Set<ProjectRef> excludes )
    {
        this( scope, scopeTransitivity, includeManaged, includeConcrete, true, excludes );
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity, final boolean includeManaged,
                             final boolean includeConcrete, final boolean useImpliedScopes, final Set<ProjectRef> excludes )
    {
        super( RelationshipType.DEPENDENCY, true, includeManaged, includeConcrete );
        this.useImpliedScopes = useImpliedScopes;
        this.scope = scope == null ? DependencyScope.test : scope;
        this.scopeTransitivity = scopeTransitivity;
        if ( excludes != null )
        {
            for ( final ProjectRef pr : excludes )
            {
                this.excludes.add( pr.asProjectRef() );
            }
        }
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        final DependencyRelationship dr = (DependencyRelationship) rel;
        if ( RelationshipUtils.isExcluded( dr.getTarget(), excludes ) )
        {
            return false;
        }

        if ( scope != null )
        {
            if ( useImpliedScopes && !scope.implies( dr.getScope() ) )
            {
                return false;
            }
            else if ( !useImpliedScopes && scope != dr.getScope() )
            {
                return false;
            }
        }

        if ( !isManagedInfoIncluded() && dr.isManaged() )
        {
            return false;
        }
        else if ( !isConcreteInfoIncluded() && !dr.isManaged() )
        {
            return false;
        }

        return true;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        DependencyRelationship dr = null;
        if ( ( parent instanceof DependencyRelationship ) )
        {
            dr = (DependencyRelationship) parent;
        }

        final DependencyScope nextScope = scopeTransitivity.getChildFor( scope );
        Set<ProjectRef> newExcludes = dr == null ? null : dr.getExcludes();
        if ( nextScope != scope || ( newExcludes != null && !newExcludes.isEmpty() ) )
        {
            if ( excludes != null )
            {
                final Set<ProjectRef> ex = new HashSet<ProjectRef>( excludes );

                if ( newExcludes != null && !newExcludes.isEmpty() )
                {
                    ex.addAll( newExcludes );
                }

                newExcludes = ex;
            }

            return new DependencyFilter( nextScope, scopeTransitivity, isManagedInfoIncluded(), isConcreteInfoIncluded(), useImpliedScopes,
                                         newExcludes );
        }

        return this;
    }

    public boolean isUseImpliedScopes()
    {
        return useImpliedScopes;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( excludes == null ) ? 0 : excludes.hashCode() );
        result = prime * result + ( ( scope == null ) ? 0 : scope.hashCode() );
        result = prime * result + ( ( scopeTransitivity == null ) ? 0 : scopeTransitivity.hashCode() );
        result = prime * result + ( useImpliedScopes ? 1231 : 1237 );
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
        final DependencyFilter other = (DependencyFilter) obj;
        if ( excludes == null )
        {
            if ( other.excludes != null )
            {
                return false;
            }
        }
        else if ( !excludes.equals( other.excludes ) )
        {
            return false;
        }
        if ( scope != other.scope )
        {
            return false;
        }
        if ( scopeTransitivity != other.scopeTransitivity )
        {
            return false;
        }
        if ( useImpliedScopes != other.useImpliedScopes )
        {
            return false;
        }
        return true;
    }

    @Override
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( "scope:" );
        sb.append( scope.realName() );
        sb.append( ",transitivity: " )
          .append( scopeTransitivity.name() );
        sb.append( ",implied-scopes: " )
          .append( isUseImpliedScopes() );

        if ( excludes != null && !excludes.isEmpty() )
        {
            sb.append( ",excludes:{" );
            boolean first = true;
            for ( final ProjectRef exclude : excludes )
            {
                if ( !first )
                {
                    sb.append( ',' );
                }

                first = false;
                sb.append( exclude );
            }

            sb.append( "}" );
        }
    }

}
