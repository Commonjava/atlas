package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipPathComparator;
import org.commonjava.maven.atlas.graph.spi.jung.model.JungGraphPath;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphPathInfo
    implements Comparable<GraphPathInfo>, Iterable<ProjectRelationship<?>>
{

    private static final RelationshipPathComparator COMP = RelationshipPathComparator.INSTANCE;

    private final List<ProjectRelationship<?>> pathElements;

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    private final ProjectVersionRef target;

    private final JungGraphPath path;

    private final GraphView view;

    public GraphPathInfo( final ProjectVersionRef root, final GraphView view, final JungGraphPath path )
    {
        this.target = root;
        this.view = view;
        this.path = path;
        this.pathElements = null;
        this.filter = view.getFilter();
        this.mutator = view.getMutator();
    }

    public GraphPathInfo( final ProjectVersionRef target, final List<ProjectRelationship<?>> pathElements, final ProjectRelationshipFilter filter,
                          final GraphMutator mutator, final GraphView view, final JungGraphPath path )
    {
        this.target = target;
        this.pathElements = pathElements;
        this.filter = filter;
        this.mutator = mutator;
        this.view = view;
        this.path = path;
    }

    public List<ProjectRelationship<?>> getPathElements()
    {
        return pathElements;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public GraphMutator getMutator()
    {
        return mutator;
    }

    public boolean isEmpty()
    {
        // if target != null but path is null, then this is the same as the old SelfEdge...
        return ( target == null && pathElements == null ) || ( pathElements != null && pathElements.isEmpty() );
    }

    public ProjectVersionRef getTarget()
    {
        return target;
    }

    public GraphPathInfo getChildPath( ProjectRelationship<?> edge )
    {
        if ( filter != null && !filter.accept( edge ) )
        {
            return null;
        }

        if ( mutator != null )
        {
            edge = mutator.selectFor( edge, path, view );
        }

        final List<ProjectRelationship<?>> nextElements = new ArrayList<ProjectRelationship<?>>();
        if ( pathElements != null )
        {
            nextElements.addAll( pathElements );
        }

        nextElements.add( edge );

        final ProjectRelationshipFilter nextFilter = filter == null ? null : filter.getChildFilter( edge );
        final GraphMutator nextMutator = mutator == null ? null : mutator.getMutatorFor( edge, view );

        final ProjectVersionRef targetRef = edge.getTarget()
                                                .asProjectVersionRef();

        return new GraphPathInfo( edge.getTarget()
                                      .asProjectVersionRef(), nextElements, nextFilter, nextMutator, view, new JungGraphPath( path, targetRef ) );
    }

    public ProjectRelationship<?> getTargetEdge()
    {
        return pathElements == null || pathElements.isEmpty() ? null : pathElements.get( pathElements.size() - 1 );
    }

    @Override
    public int compareTo( final GraphPathInfo other )
    {
        if ( pathElements == null && other.pathElements == null )
        {
            return 0;
        }
        else if ( pathElements != null && other.pathElements == null )
        {
            return 1;
        }
        else if ( pathElements == null )
        {
            return -1;
        }

        return COMP.compare( pathElements, other.pathElements );
    }

    @Override
    public Iterator<ProjectRelationship<?>> iterator()
    {
        return pathElements == null ? Collections.<ProjectRelationship<?>> emptySet()
                                                 .iterator() : pathElements.iterator();
    }

    public boolean hasCycle()
    {
        if ( pathElements == null || pathElements.isEmpty() )
        {
            return false;
        }

        for ( final ProjectRelationship<?> item : pathElements )
        {
            if ( item.getDeclaring()
                     .equals( target ) )
            {
                return true;
            }
        }

        return false;
    }

}
