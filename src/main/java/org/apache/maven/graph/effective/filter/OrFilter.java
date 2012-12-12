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

}
