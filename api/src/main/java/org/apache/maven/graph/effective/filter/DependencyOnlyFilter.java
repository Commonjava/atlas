/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

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
