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

import java.util.Collection;
import java.util.List;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class OrFilter
    extends AbstractAggregatingFilter
{

    public OrFilter( final Collection<ProjectRelationshipFilter> filters )
    {
        super( filters );
    }

    public OrFilter( final ProjectRelationshipFilter... filters )
    {
        super( filters );
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean accepted = false;
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            accepted = accepted || filter.accept( rel );
            if ( accepted )
            {
                break;
            }
        }

        return accepted;
    }

    @Override
    protected AbstractAggregatingFilter newChildFilter( final List<ProjectRelationshipFilter> childFilters )
    {
        return new OrFilter( childFilters );
    }

    public void render( final StringBuilder sb )
    {
        final List<ProjectRelationshipFilter> filters = getFilters();
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "[" );
        boolean first = true;
        for ( final ProjectRelationshipFilter filter : filters )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                sb.append( " || " );
            }

            filter.render( sb );
        }
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
