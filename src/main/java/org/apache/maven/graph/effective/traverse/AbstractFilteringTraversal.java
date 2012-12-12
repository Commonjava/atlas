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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public abstract class AbstractFilteringTraversal
    extends AbstractTraversal
{

    private final Map<EdgeId, ProjectRelationshipFilter> filtersInReserve =
        new HashMap<EdgeId, ProjectRelationshipFilter>();

    private ProjectRelationshipFilter currentFilter;

    protected AbstractFilteringTraversal()
    {
    }

    protected AbstractFilteringTraversal( final ProjectRelationshipFilter filter )
    {
        currentFilter = filter;
    }

    protected AbstractFilteringTraversal( final ProjectRelationshipFilter filter, final TraversalType... types )
    {
        super( types );
        currentFilter = filter;
    }

    protected abstract boolean shouldTraverseEdge( ProjectRelationship<?> relationship,
                                                   List<ProjectRelationship<?>> path, int pass );

    protected void edgeTraversalFinished( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
    }

    @Override
    public final void edgeTraversed( final ProjectRelationship<?> relationship,
                                     final List<ProjectRelationship<?>> path, final int pass )
    {
        final EdgeId id = new EdgeId( relationship, path );
        final ProjectRelationshipFilter filter = filtersInReserve.get( id );

        currentFilter = filter;

        edgeTraversalFinished( relationship, path, pass );
    }

    @Override
    public final boolean traverseEdge( final ProjectRelationship<?> relationship,
                                       final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( currentFilter != null && !currentFilter.accept( relationship ) )
        {
            return false;
        }

        if ( shouldTraverseEdge( relationship, path, pass ) )
        {
            if ( currentFilter != null )
            {
                final EdgeId id = new EdgeId( relationship, path );
                filtersInReserve.put( id, currentFilter );

                currentFilter = currentFilter.getChildFilter( relationship );
            }

            return true;
        }

        return false;
    }

    public static final class EdgeId
    {

        private final ProjectRelationship<?> top;

        private final List<ProjectRelationship<?>> path;

        public EdgeId( final ProjectRelationship<?> top, final List<ProjectRelationship<?>> path )
        {
            this.top = top;
            this.path = path;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( path == null ) ? 0 : path.hashCode() );
            result = prime * result + ( ( top == null ) ? 0 : top.hashCode() );
            return result;
        }

        @Override
        public boolean equals( final Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( obj == null )
            {
                return false;
            }
            if ( getClass() != obj.getClass() )
            {
                return false;
            }
            final EdgeId other = (EdgeId) obj;
            if ( path == null )
            {
                if ( other.path != null )
                {
                    return false;
                }
            }
            else if ( !path.equals( other.path ) )
            {
                return false;
            }
            if ( top == null )
            {
                if ( other.top != null )
                {
                    return false;
                }
            }
            else if ( !top.equals( other.top ) )
            {
                return false;
            }
            return true;
        }

    }

}
