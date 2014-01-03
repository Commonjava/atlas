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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public abstract class AbstractAggregatingFilter
    implements ProjectRelationshipFilter, Iterable<ProjectRelationshipFilter>
{
    private final List<? extends ProjectRelationshipFilter> filters;

    protected AbstractAggregatingFilter( final Collection<? extends ProjectRelationshipFilter> filters )
    {
        this.filters = new ArrayList<ProjectRelationshipFilter>( filters );
    }

    protected AbstractAggregatingFilter( final ProjectRelationshipFilter... filters )
    {
        this.filters = new ArrayList<ProjectRelationshipFilter>( Arrays.asList( filters ) );
    }

    public final List<? extends ProjectRelationshipFilter> getFilters()
    {
        return filters;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        final List<ProjectRelationshipFilter> childFilters = new ArrayList<ProjectRelationshipFilter>();
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            //            if ( filter.accept( parent ) )
            //            {
            childFilters.add( filter.getChildFilter( parent ) );
            //            }
        }

        return newChildFilter( childFilters );
    }

    protected abstract AbstractAggregatingFilter newChildFilter( List<ProjectRelationshipFilter> childFilters );

    @Override
    public Iterator<ProjectRelationshipFilter> iterator()
    {
        return new ArrayList<ProjectRelationshipFilter>( filters ).iterator();
    }

}
