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

import static org.apache.commons.lang.StringUtils.join;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceListener;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphView
{

    private final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

    private final GraphWorkspace workspace;

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    private transient String longId;

    private transient String shortId;

    private Map<ProjectRef, ProjectVersionRef> selections;

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
        return mutator;
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
        return String.format( "GraphView [\n  roots=%s\n  workspace=%s\n  filter=%s\n  mutator=%s\n]", roots, workspace, filter, mutator );
    }

    public GraphDatabaseDriver getDatabase()
    {
        return workspace == null ? null : workspace.getDatabase();
    }

    //    public void setSelections( final VersionManager selections )
    //    {
    //        if ( workspace != null )
    //        {
    //            workspace.setSelections( selections );
    //        }
    //    }
    //
    //    public void setFilter( final ProjectRelationshipFilter filter )
    //    {
    //        this.filter = filter;
    //    }
    //
    //    public void setMutator( final GraphMutator mutator )
    //    {
    //        this.mutator = mutator;
    //    }
    //
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            final String abbreviatedPackage = getClass().getPackage()
                                                        .getName()
                                                        .replaceAll( "([a-zA-Z])[a-zA-Z]+", "$1" );

            sb.append( abbreviatedPackage )
              .append( '.' )
              .append( getClass().getSimpleName() )
              .append( "(workspace:" )
              .append( workspace.getId() )
              .append( ",roots:" )
              .append( join( roots, "," ) )
              .append( ",filter:" )
              .append( filter == null ? "none" : filter.getLongId() )
              .append( ",mutator:" )
              .append( mutator == null ? "none" : mutator.getLongId() )
              .append( ",selections:\n  " )
              .append( renderSelections() );

            sb.append( ")" );

            longId = sb.toString();
        }

        return longId;
    }

    public String getShortId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }

    public void touch()
    {
        workspace.touch();
    }

    public GraphWorkspace addActivePomLocation( final URI location )
    {
        return workspace.addActivePomLocation( location );
    }

    public GraphWorkspace addActivePomLocations( final Collection<URI> locations )
    {
        return workspace.addActivePomLocations( locations );
    }

    public GraphWorkspace addActivePomLocations( final URI... locations )
    {
        return workspace.addActivePomLocations( locations );
    }

    public GraphWorkspace addActiveSources( final Collection<URI> sources )
    {
        return workspace.addActiveSources( sources );
    }

    public GraphWorkspace addActiveSources( final URI... sources )
    {
        return workspace.addActiveSources( sources );
    }

    public GraphWorkspace addActiveSource( final URI source )
    {
        return workspace.addActiveSource( source );
    }

    public long getLastAccess()
    {
        return workspace.getLastAccess();
    }

    public String setProperty( final String key, final String value )
    {
        return workspace.setProperty( key, value );
    }

    public String removeProperty( final String key )
    {
        return workspace.removeProperty( key );
    }

    public String getProperty( final String key )
    {
        return workspace.getProperty( key );
    }

    public String getProperty( final String key, final String def )
    {
        return workspace.getProperty( key, def );
    }

    public final String getId()
    {
        return workspace.getId();
    }

    public final Set<URI> getActivePomLocations()
    {
        return workspace.getActivePomLocations();
    }

    public final Set<URI> getActiveSources()
    {
        return workspace.getActiveSources();
    }

    public final Iterable<URI> activePomLocations()
    {
        return workspace.activePomLocations();
    }

    public final Iterable<URI> activeSources()
    {
        return workspace.activeSources();
    }

    public final ProjectVersionRef selectVersion( final ProjectRef ref, final ProjectVersionRef selected )
    {
        if ( selections == null )
        {
            selections = new HashMap<ProjectRef, ProjectVersionRef>();
        }

        final ProjectVersionRef old = selections.get( ref );
        if ( old != null )
        {
            return old;
        }

        this.selections.put( ref, selected );
        return selected;
    }

    public final void clearSelections()
    {
        if ( selections != null )
        {
            selections.clear();
        }
    }

    public final ProjectVersionRef getSelection( final ProjectRef ref )
    {
        if ( selections == null )
        {
            return null;
        }

        ProjectVersionRef selected = selections.get( ref );
        if ( selected == null )
        {
            selected = selections.get( ref.asProjectRef() );
        }

        return selected;
    }

    public final boolean isOpen()
    {
        return workspace.isOpen();
    }

    public GraphWorkspace addListener( final GraphWorkspaceListener listener )
    {
        return workspace.addListener( listener );
    }

    public boolean hasSelection( final ProjectVersionRef ref )
    {
        return selections == null ? false : selections.containsKey( ref ) || selections.containsKey( ref.asProjectRef() );
    }

    public String renderSelections()
    {
        if ( selections == null )
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        for ( final Entry<ProjectRef, ProjectVersionRef> entry : selections.entrySet() )
        {
            final ProjectRef key = entry.getKey();
            final ProjectVersionRef value = entry.getValue();

            sb.append( "\n  " )
              .append( key )
              .append( " => " )
              .append( value );
        }

        return sb.toString();
    }
}
