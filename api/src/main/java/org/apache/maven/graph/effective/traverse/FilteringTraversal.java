package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class FilteringTraversal
    extends AbstractFilteringTraversal
{

    private final List<ProjectRelationship<?>> captured = new ArrayList<ProjectRelationship<?>>();

    private final boolean doCapture;

    public FilteringTraversal( final ProjectRelationshipFilter filter )
    {
        this( filter, false );
    }

    public FilteringTraversal( final ProjectRelationshipFilter filter, final boolean doCapture )
    {
        super( filter );
        this.doCapture = doCapture;
    }

    public List<ProjectRelationship<?>> getCapturedRelationships()
    {
        return captured;
    }

    public List<ProjectVersionRef> getCapturedProjects( final boolean unique )
    {
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : captured )
        {
            final ProjectVersionRef d = rel.getDeclaring();
            final ProjectVersionRef t = rel.getTarget()
                                           .asProjectVersionRef();

            if ( !unique || !refs.contains( d ) )
            {
                refs.add( d );
            }

            if ( !unique || !refs.contains( t ) )
            {
                refs.add( t );
            }
        }

        return refs;
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( doCapture )
        {
            captured.add( relationship );
        }
        return true;
    }

}
