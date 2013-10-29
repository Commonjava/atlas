package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractFilteringTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class PathMappingTraversal
    extends AbstractFilteringTraversal
{

    private final Map<ProjectVersionRef, PathState> pathStates;

    public PathMappingTraversal( final ProjectRelationshipFilter filter, final Map<ProjectVersionRef, PathState> pathStates )
    {
        super( filter );
        this.pathStates = pathStates;
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        return true;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        final List<ProjectRelationship<?>> realPath = new ArrayList<>( path );
        realPath.add( relationship );

        final ProjectVersionRef target = relationship.getTarget()
                                                     .asProjectVersionRef();
        PathState state = pathStates.get( target );
        if ( state == null )
        {
            state = new PathState( target );
            pathStates.put( target, state );
        }

        final ProjectRelationshipFilter filter = constructFilter( path );
        if ( filter != null && !filter.accept( relationship ) )
        {
            state.addInvisiblePath( realPath, filter );
            return false;
        }

        state.addVisiblePath( realPath, filter );
        return true;
    }

}
