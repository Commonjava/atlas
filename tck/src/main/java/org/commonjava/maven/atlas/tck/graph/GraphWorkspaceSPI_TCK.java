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
package org.commonjava.maven.atlas.tck.graph;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.junit.Test;

public abstract class GraphWorkspaceSPI_TCK
    extends AbstractSPI_TCK
{

    @Test
    public void createWorkspaceAndRetrieveById()
        throws Exception
    {
        final GraphWorkspace ws = getManager().createWorkspace( new GraphWorkspaceConfiguration() );
        System.out.println( "wsid: " + ws.getId() );

        assertThat( ws, notNullValue() );

        logger.info( "Created workspace: {}", ws );

        final GraphWorkspace result = getManager().getWorkspace( ws.getId() );

        logger.info( "Retrieved all workspaces: {}", result );

        assertThat( result, notNullValue() );
        assertThat( result.getId(), equalTo( ws.getId() ) );
        assertThat( result.equals( ws ), equalTo( true ) );
    }

    @Test
    public void createWorkspaceAndFindInAllWorkspacesListing()
        throws Exception
    {
        final GraphWorkspace ws = getManager().createWorkspace( new GraphWorkspaceConfiguration() );
        System.out.println( "wsid: " + ws.getId() );

        assertThat( ws, notNullValue() );

        logger.info( "Created workspace: {}", ws );

        final Set<GraphWorkspace> all = getManager().getAllWorkspaces();

        logger.info( "Retrieved all workspaces: {}", all );

        assertThat( all, notNullValue() );
        assertThat( all.size(), equalTo( 1 ) );
        assertThat( all.contains( ws ), equalTo( true ) );
    }

}
