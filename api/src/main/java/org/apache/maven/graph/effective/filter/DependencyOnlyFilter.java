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
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class DependencyOnlyFilter
    implements ProjectRelationshipFilter
{

    // if unspecified, include all dependencies.
    private DependencyScope scope = DependencyScope.test;

    private boolean includeManaged = false;

    private boolean includeConcrete = true;

    private boolean useImpliedScope = true;

    public DependencyOnlyFilter()
    {
    }

    public DependencyOnlyFilter( final DependencyScope scope )
    {
        if ( scope != null )
        {
            this.scope = scope;
        }
    }

    public DependencyOnlyFilter( final DependencyScope scope, final boolean includeManaged,
                                 final boolean includeConcrete, final boolean useImpliedScope )
    {
        if ( scope != null )
        {
            this.scope = scope;
        }
        this.includeConcrete = includeConcrete;
        this.includeManaged = includeManaged;
        this.useImpliedScope = useImpliedScope;
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        if ( rel instanceof DependencyRelationship )
        {
            if ( this.scope == null )
            {
                return true;
            }

            final DependencyScope scope = ( (DependencyRelationship) rel ).getScope();
            if ( scope == this.scope || ( useImpliedScope && this.scope.implies( scope ) ) )
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
          .append( includeManaged )
          .append( ", concrete: " )
          .append( includeConcrete );
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
