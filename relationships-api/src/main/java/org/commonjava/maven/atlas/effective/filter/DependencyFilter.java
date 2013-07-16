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
package org.commonjava.maven.atlas.effective.filter;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.common.DependencyScope;
import org.commonjava.maven.atlas.common.RelationshipType;
import org.commonjava.maven.atlas.common.ScopeTransitivity;
import org.commonjava.maven.atlas.common.ref.ProjectRef;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

public class DependencyFilter
    extends AbstractTypedFilter
{

    private final DependencyScope scope;

    private final ScopeTransitivity scopeTransitivity;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    public DependencyFilter()
    {
        this( DependencyScope.test, ScopeTransitivity.maven, false, true, null );
    }

    public DependencyFilter( final DependencyScope scope )
    {
        this( scope, ScopeTransitivity.maven, false, true, null );
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity,
                             final boolean includeManaged, final boolean includeConcrete, final Set<ProjectRef> excludes )
    {
        super( RelationshipType.DEPENDENCY, true, includeManaged, includeConcrete );
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

    public DependencyFilter( final DependencyFilter parent, final DependencyRelationship parentRel )
    {
        super( RelationshipType.DEPENDENCY, true, parent.isManagedInfoIncluded(), parent.isConcreteInfoIncluded() );
        this.scopeTransitivity = parent.scopeTransitivity;
        this.scope = parent.scope == null ? parent.scope : scopeTransitivity.getChildFor( parent.scope );

        if ( parent.excludes != null )
        {
            excludes.addAll( parent.excludes );
        }

        if ( parentRel != null )
        {
            final Set<ProjectRef> excl = parentRel.getExcludes();
            if ( excl != null && !excl.isEmpty() )
            {
                for ( final ProjectRef pr : excl )
                {
                    this.excludes.add( pr.asProjectRef() );
                }
            }
        }
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        final DependencyRelationship dr = (DependencyRelationship) rel;
        if ( excludes.contains( dr.getTarget()
                                  .asProjectRef() ) )
        {
            return false;
        }

        if ( scope != null )
        {
            if ( !scope.implies( dr.getScope() ) )
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

        return new DependencyFilter( this, dr );
    }

    @Override
    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }

        sb.append( "DEPENDENCIES[scope: " );
        sb.append( scope.realName() );
        sb.append( ", transitivity: " )
          .append( scopeTransitivity.name() );
        sb.append( ", managed: " )
          .append( isManagedInfoIncluded() )
          .append( ", concrete: " )
          .append( isConcreteInfoIncluded() );
        if ( excludes != null && !excludes.isEmpty() )
        {
            sb.append( ", exclude: {" );
            boolean first = true;
            for ( final ProjectRef exclude : excludes )
            {
                if ( !first )
                {
                    sb.append( ", " );
                }

                first = false;
                sb.append( exclude );
            }

            sb.append( "}" );
        }
        sb.append( "]" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
