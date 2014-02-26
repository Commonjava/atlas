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

import java.util.Arrays;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;

public class DependencyOnlyFilter
    extends AbstractTypedFilter
{

    // if unspecified, include all dependencies.
    private final DependencyScope[] scopes;

    private final boolean useImpliedScope;

    public DependencyOnlyFilter()
    {
        this( false, true, true, DependencyScope.test );
    }

    public DependencyOnlyFilter( final DependencyScope... scopes )
    {
        this( false, true, true, scopes );
    }

    public DependencyOnlyFilter( final boolean includeManaged, final boolean includeConcrete, final boolean useImpliedScope,
                                 final DependencyScope... scopes )
    {
        super( RelationshipType.DEPENDENCY, false, includeManaged, includeConcrete );

        this.scopes = scopes;
        this.useImpliedScope = useImpliedScope;
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        if ( this.scopes == null || this.scopes.length < 1 )
        {
            return true;
        }

        final DependencyRelationship dr = (DependencyRelationship) rel;
        final DependencyScope scope = dr.getScope();
        for ( final DependencyScope s : this.scopes )
        {
            if ( scope == s || ( useImpliedScope && s.implies( scope ) ) )
            {
                if ( ( dr.isManaged() && isManagedInfoIncluded() ) || ( !dr.isManaged() && isConcreteInfoIncluded() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return NoneFilter.INSTANCE;
    }

    @Override
    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }

        sb.append( "DEPENDENCIES ONLY[scopes: (" );
        boolean first = true;
        for ( final DependencyScope scope : scopes )
        {
            if ( !first )
            {
                sb.append( ", " );
            }

            sb.append( scope.realName() );
            first = false;
        }

        sb.append( "), managed: " )
          .append( isManagedInfoIncluded() )
          .append( ", concrete: " )
          .append( isConcreteInfoIncluded() )
          .append( ", implied scopes: " )
          .append( useImpliedScope )
          .append( "]" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode( scopes );
        result = prime * result + ( useImpliedScope ? 1231 : 1237 );
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
        final DependencyOnlyFilter other = (DependencyOnlyFilter) obj;
        if ( !Arrays.equals( scopes, other.scopes ) )
        {
            return false;
        }
        if ( useImpliedScope != other.useImpliedScope )
        {
            return false;
        }
        return true;
    }

}
