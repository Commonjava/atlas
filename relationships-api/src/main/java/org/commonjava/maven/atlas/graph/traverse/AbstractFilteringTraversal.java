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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public abstract class AbstractFilteringTraversal
    extends AbstractTraversal
{

    private final ProjectRelationshipFilter rootFilter;

    private final Set<ProjectRelationship<?>> seen = new HashSet<ProjectRelationship<?>>();

    protected AbstractFilteringTraversal()
    {
        rootFilter = AnyFilter.INSTANCE;
    }

    protected AbstractFilteringTraversal( final ProjectRelationshipFilter filter )
    {
        rootFilter = filter;
    }

    protected AbstractFilteringTraversal( final ProjectRelationshipFilter filter, final TraversalType... types )
    {
        super( types );
        rootFilter = filter;
    }

    protected AbstractFilteringTraversal( final ProjectRelationshipFilter filter, final int passes, final TraversalType... types )
    {
        super( passes, types );
        rootFilter = filter;
    }

    protected abstract boolean shouldTraverseEdge( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

    protected void edgeTraversalFinished( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
    }

    public final ProjectRelationshipFilter getRootFilter()
    {
        return rootFilter;
    }

    @Override
    public final void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        edgeTraversalFinished( relationship, path, pass );
    }

    @Override
    public final boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( !preCheck( relationship, path, pass ) )
        {
            return false;
        }

        seen.add( relationship );

        final boolean ok = shouldTraverseEdge( relationship, path, pass );

        return ok;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        boolean result = true;
        if ( seen.contains( relationship ) )
        {
            result = false;
        }

        final ProjectRelationshipFilter filter = constructFilter( path );
        if ( result && filter != null && !filter.accept( relationship ) )
        {
            seen.add( relationship );
            result = false;
        }

        return result;
    }

    private ProjectRelationshipFilter constructFilter( final List<ProjectRelationship<?>> path )
    {
        if ( rootFilter == null )
        {
            return null;
        }

        ProjectRelationshipFilter filter = rootFilter;
        for ( final ProjectRelationship<?> rel : path )
        {
            if ( !filter.accept( rel ) )
            {
                return NoneFilter.INSTANCE;
            }
            else
            {
                filter = filter.getChildFilter( rel );
            }
        }

        return filter;
    }

}
