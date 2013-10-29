package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class PathState
{

    private final ProjectVersionRef ref;

    private final Map<List<ProjectRelationship<?>>, ProjectRelationshipFilter> visibleFiltersByPath = new LinkedHashMap<>();

    private final Map<List<ProjectRelationship<?>>, ProjectRelationshipFilter> invisibleFiltersByPath = new LinkedHashMap<>();

    public PathState( final ProjectVersionRef ref )
    {
        this.ref = ref;
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    public boolean isVisible()
    {
        return !visibleFiltersByPath.isEmpty();
    }

    public boolean isInvisible()
    {
        return !invisibleFiltersByPath.isEmpty();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( ref == null ) ? 0 : ref.hashCode() );
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
        final PathState other = (PathState) obj;
        if ( ref == null )
        {
            if ( other.ref != null )
            {
                return false;
            }
        }
        else if ( !ref.equals( other.ref ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "PathState [%s]", ref );
    }

    public void addVisiblePath( final List<ProjectRelationship<?>> path, final ProjectRelationshipFilter filter )
    {
        visibleFiltersByPath.put( path, filter );
    }

    public void addInvisiblePath( final List<ProjectRelationship<?>> path, final ProjectRelationshipFilter filter )
    {
        invisibleFiltersByPath.put( path, filter );
    }

    public boolean containsVisibleRelationship( final ProjectRelationship<?> rel )
    {

        for ( final List<ProjectRelationship<?>> path : visibleFiltersByPath.keySet() )
        {
            if ( path.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    public Collection<? extends List<ProjectRelationship<?>>> getVisiblePaths()
    {
        return visibleFiltersByPath.keySet();
    }

    public Collection<? extends ProjectRelationship<?>> getVisibleTargetingRelationships()
    {
        final Set<ProjectRelationship<?>> result = new HashSet<>( visibleFiltersByPath.size() );

        for ( final List<ProjectRelationship<?>> path : visibleFiltersByPath.keySet() )
        {
            if ( !path.isEmpty() )
            {
                result.add( path.get( path.size() - 1 ) );
            }
        }

        return result;
    }

    public Collection<ProjectRelationshipFilter> getVisiblePathFilters()
    {
        return visibleFiltersByPath.values();
    }

}
