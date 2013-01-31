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
package org.apache.maven.graph.effective.traverse;

import java.util.List;

import org.apache.maven.graph.effective.filter.NoneFilter;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public abstract class AbstractFilteringTraversal
    extends AbstractTraversal
{

    private final ProjectRelationshipFilter rootFilter;

    protected AbstractFilteringTraversal()
    {
        rootFilter = null;
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

    protected abstract boolean shouldTraverseEdge( ProjectRelationship<?> relationship,
                                                   List<ProjectRelationship<?>> path, int pass );

    protected void edgeTraversalFinished( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
    }

    protected final ProjectRelationshipFilter getFilter()
    {
        return rootFilter;
    }

    @Override
    public final void edgeTraversed( final ProjectRelationship<?> relationship,
                                     final List<ProjectRelationship<?>> path, final int pass )
    {
        edgeTraversalFinished( relationship, path, pass );
    }

    @Override
    public final boolean traverseEdge( final ProjectRelationship<?> relationship,
                                       final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( !preCheck( relationship, path, pass ) )
        {
            return false;
        }

        if ( shouldTraverseEdge( relationship, path, pass ) )
        {
            return true;
        }

        return false;
    }

    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                             final int pass )
    {
        final ProjectRelationshipFilter filter = constructFilter( path );
        if ( filter != null && !filter.accept( relationship ) )
        {
            return false;
        }

        return true;
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
                return new NoneFilter();
            }
            else
            {
                filter = filter.getChildFilter( rel );
            }
        }

        return filter;
    }

}
