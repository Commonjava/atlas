/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.mutate.VersionManager;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphView
{

    private final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

    private final GraphWorkspace workspace;

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    private final WeakHashMap<String, Object> cache = new WeakHashMap<String, Object>();

    public GraphView( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                      final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots.addAll( roots );
        this.workspace = workspace;
        this.mutator = mutator;
    }

    public GraphView( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                      final ProjectVersionRef... roots )
    {
        this.filter = filter;
        this.roots.addAll( Arrays.asList( roots ) );
        this.workspace = workspace;
        this.mutator = mutator;
    }

    public GraphView( final GraphWorkspace workspace, final Collection<ProjectVersionRef> roots )
    {
        this( workspace, null, null, roots );
    }

    public GraphView( final GraphWorkspace workspace, final ProjectVersionRef... roots )
    {
        this( workspace, null, null, roots );
    }

    public GraphMutator getMutator()
    {
        return mutator == null ? new ManagedDependencyMutator( this, true ) : mutator;
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
        return workspace == null ? null : workspace.getDatabase();
    }

    public VersionManager getSelections()
    {
        return workspace == null ? null : workspace.getSelections();
    }

    public void setSelections( final VersionManager selections )
    {
        if ( workspace != null )
        {
            workspace.setSelections( selections );
        }
    }
}
