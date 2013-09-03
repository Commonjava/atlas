package org.commonjava.maven.atlas.graph.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphView
{

    public static final GraphView GLOBAL = new GraphView( null );

    private final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

    private final GraphWorkspace workspace;

    private final ProjectRelationshipFilter filter;

    private final WeakHashMap<String, Object> cache = new WeakHashMap<String, Object>();

    public GraphView( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots.addAll( roots );
        this.workspace = workspace;
    }

    public GraphView( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef... roots )
    {
        this.filter = filter;
        this.roots.addAll( Arrays.asList( roots ) );
        this.workspace = workspace;
    }

    public GraphView( final GraphWorkspace workspace, final Collection<ProjectVersionRef> roots )
    {
        this.filter = null;
        this.roots.addAll( roots );
        this.workspace = workspace;
    }

    public GraphView( final GraphWorkspace workspace, final ProjectVersionRef... roots )
    {
        this.filter = null;
        this.roots.addAll( Arrays.asList( roots ) );
        this.workspace = workspace;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public GraphWorkspace getWorkspace()
    {
        return workspace;
    }

    public Object setCache( final String key, final Object value )
    {
        return cache.put( key, value );
    }

    public Object removeCache( final String key )
    {
        return cache.remove( key );
    }

    public <T> T getCache( final String key, final Class<T> type )
    {
        final Object value = cache.get( key );
        if ( value != null )
        {
            return type.cast( value );
        }

        return null;
    }

    public <T> T getCache( final String key, final Class<T> type, final T def )
    {
        final Object value = cache.get( key );
        if ( value != null )
        {
            return type.cast( value );
        }

        return def;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( filter == null ) ? 0 : filter.hashCode() );
        result = prime * result + ( ( roots == null ) ? 0 : roots.hashCode() );
        result = prime * result + ( ( workspace == null ) ? 0 : workspace.hashCode() );
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
        final GraphView other = (GraphView) obj;
        if ( filter == null )
        {
            if ( other.filter != null )
            {
                return false;
            }
        }
        else if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        if ( roots == null )
        {
            if ( other.roots != null )
            {
                return false;
            }
        }
        else if ( !roots.equals( other.roots ) )
        {
            return false;
        }
        if ( workspace == null )
        {
            if ( other.workspace != null )
            {
                return false;
            }
        }
        else if ( !workspace.equals( other.workspace ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphView [\n  roots=%s\n  workspace=%s\n  filter=%s\n]", roots, workspace, filter );
    }

    public GraphDatabaseDriver getDatabase()
    {
        return workspace.getDatabase();
    }
}
