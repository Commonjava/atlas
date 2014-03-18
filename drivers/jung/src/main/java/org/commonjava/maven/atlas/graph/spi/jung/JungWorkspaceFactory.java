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
package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;

public class JungWorkspaceFactory
    implements GraphWorkspaceFactory
{

    private final Map<String, GraphWorkspace> workspaces = new HashMap<String, GraphWorkspace>();

    @Override
    public GraphWorkspace createWorkspace( final String id, final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        if ( workspaces.containsKey( id ) )
        {
            throw new GraphDriverException( "Workspace already exists: %s. Cannot create workspace.", id );
        }

        final GraphWorkspace ws = new GraphWorkspace( id, new JungEGraphDriver( config ) );
        workspaces.put( ws.getId(), ws );
        return ws;
    }

    @Override
    public GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final GraphWorkspace ws = new GraphWorkspace( Long.toString( System.currentTimeMillis() ), new JungEGraphDriver( config ) );
        workspaces.put( ws.getId(), ws );
        return ws;
    }

    @Override
    public boolean deleteWorkspace( final String id )
    {
        return workspaces.remove( id ) != null;
    }

    @Override
    public void storeWorkspace( final GraphWorkspace workspace )
        throws GraphDriverException
    {
        // currently just stored in memory...
    }

    @Override
    public GraphWorkspace loadWorkspace( final String id )
        throws GraphDriverException
    {
        return workspaces.get( id );
    }

    @Override
    public Set<GraphWorkspace> loadAllWorkspaces( final Set<String> excluded )
    {
        final Set<GraphWorkspace> result = new HashSet<GraphWorkspace>( workspaces.values() );
        for ( final Iterator<GraphWorkspace> it = result.iterator(); it.hasNext(); )
        {
            final GraphWorkspace ws = it.next();
            if ( excluded.contains( ws.getId() ) )
            {
                it.remove();
            }
        }

        return result;
    }

}
