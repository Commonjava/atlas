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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ScopeTransitivity;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class DependencyFilter
    implements ProjectRelationshipFilter
{

    private final DependencyScope scope;

    private ScopeTransitivity scopeTransitivity = ScopeTransitivity.maven;

    private boolean includeManaged = false;

    private boolean includeConcrete = true;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    public DependencyFilter()
    {
        this.scope = null;
    }

    public DependencyFilter( final DependencyScope scope )
    {
        this.scope = scope;
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity,
                             final boolean includeManaged, final boolean includeConcrete, final Set<ProjectRef> excludes )
    {
        this.scope = scope;
        this.scopeTransitivity = scopeTransitivity;
        this.includeConcrete = includeConcrete;
        this.includeManaged = includeManaged;
        if ( excludes != null )
        {
            this.excludes.addAll( excludes );
        }
    }

    public DependencyFilter( final DependencyFilter parent, final DependencyRelationship parentRel )
    {
        this.scopeTransitivity = parent.scopeTransitivity;
        this.includeConcrete = parent.includeConcrete;
        this.includeManaged = parent.includeManaged;
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
                excludes.addAll( excl );
            }
        }
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        if ( rel instanceof DependencyRelationship )
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

            if ( !includeManaged && dr.isManaged() )
            {
                return false;
            }
            else if ( !includeConcrete && !dr.isManaged() )
            {
                return false;
            }

            return true;
        }

        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        DependencyRelationship dr = null;
        if ( ( parent instanceof DependencyRelationship ) )
        {
            dr = (DependencyRelationship) parent;
        }

        return new DependencyFilter( this, dr );
    }

}
