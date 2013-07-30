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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;

public class DependencyOnlyFilter
    extends AbstractTypedFilter
{

    // if unspecified, include all dependencies.
    private final DependencyScope scope;

    private final boolean useImpliedScope;

    public DependencyOnlyFilter()
    {
        this( DependencyScope.test, false, true, true );
    }

    public DependencyOnlyFilter( final DependencyScope scope )
    {
        this( scope, false, true, true );
    }

    public DependencyOnlyFilter( final DependencyScope scope, final boolean includeManaged,
                                 final boolean includeConcrete, final boolean useImpliedScope )
    {
        super( RelationshipType.DEPENDENCY, false, includeManaged, includeConcrete );

        this.scope = scope == null ? DependencyScope.test : scope;
        this.useImpliedScope = useImpliedScope;
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        if ( this.scope == null )
        {
            return true;
        }

        final DependencyRelationship dr = (DependencyRelationship) rel;
        final DependencyScope scope = dr.getScope();
        if ( scope == this.scope || ( useImpliedScope && this.scope.implies( scope ) ) )
        {
            if ( ( dr.isManaged() && isManagedInfoIncluded() ) || ( !dr.isManaged() && isConcreteInfoIncluded() ) )
            {
                return true;
            }
        }

        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new NoneFilter();
    }

    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }

        sb.append( "DEPENDENCIES ONLY[scope: " );
        sb.append( scope.realName() );
        sb.append( ", managed: " )
          .append( isManagedInfoIncluded() )
          .append( ", concrete: " )
          .append( isConcreteInfoIncluded() );
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
