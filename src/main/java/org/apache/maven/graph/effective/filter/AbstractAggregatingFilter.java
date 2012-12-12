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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public abstract class AbstractAggregatingFilter
    implements ProjectRelationshipFilter
{
    private final List<ProjectRelationshipFilter> filters;

    protected AbstractAggregatingFilter( final Collection<ProjectRelationshipFilter> filters )
    {
        this.filters = new ArrayList<ProjectRelationshipFilter>( filters );
    }

    protected AbstractAggregatingFilter( final ProjectRelationshipFilter... filters )
    {
        this.filters = new ArrayList<ProjectRelationshipFilter>( Arrays.asList( filters ) );
    }

    protected final List<ProjectRelationshipFilter> getFilters()
    {
        return filters;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        final List<ProjectRelationshipFilter> childFilters = new ArrayList<ProjectRelationshipFilter>();
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            childFilters.add( filter.getChildFilter( parent ) );
        }

        return newChildFilter( childFilters );
    }

    protected abstract AbstractAggregatingFilter newChildFilter( List<ProjectRelationshipFilter> childFilters );

}
