package org.apache.maven.graph.effective.filter;

import java.util.Collection;
import java.util.List;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class AndFilter
    extends AbstractAggregatingFilter
{

    public AndFilter( final Collection<ProjectRelationshipFilter> filters )
    {
        super( filters );
    }

    public AndFilter( final ProjectRelationshipFilter... filters )
    {
        super( filters );
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean accepted = true;
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            accepted = accepted && filter.accept( rel );
            if ( !accepted )
            {
                break;
            }
        }

        return accepted;
    }

    @Override
    protected AbstractAggregatingFilter newChildFilter( final List<ProjectRelationshipFilter> childFilters )
    {
        return new AndFilter( childFilters );
    }

}
