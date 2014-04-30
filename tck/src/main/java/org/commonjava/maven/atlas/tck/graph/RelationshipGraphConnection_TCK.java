/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.tck.graph;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.junit.Test;

public abstract class RelationshipGraphConnection_TCK
    extends AbstractSPI_TCK
{

    @Test
    public void createWorkspaceAndRetrieveById()
        throws Exception
    {
        final GraphWorkspace ws = graphFactory().createWorkspace( new GraphWorkspaceConfiguration() );
        System.out.println( "wsid: " + ws.getId() );

        assertThat( ws, notNullValue() );

        logger.info( "Created workspace: {}", ws );

        final GraphWorkspace result = graphFactory().getWorkspace( ws.getId() );

        logger.info( "Retrieved all workspaces: {}", result );

        assertThat( result, notNullValue() );
        assertThat( result.getId(), equalTo( ws.getId() ) );
        assertThat( result.equals( ws ), equalTo( true ) );
    }

    @Test
    public void createWorkspaceAndFindInAllWorkspacesListing()
        throws Exception
    {
        final GraphWorkspace ws = graphFactory().createWorkspace( new GraphWorkspaceConfiguration() );
        System.out.println( "wsid: " + ws.getId() );

        assertThat( ws, notNullValue() );

        logger.info( "Created workspace: {}", ws );

        final Set<GraphWorkspace> all = graphFactory().getAllWorkspaces();

        logger.info( "Retrieved all workspaces: {}", all );

        assertThat( all, notNullValue() );
        assertThat( all.size(), equalTo( 1 ) );
        assertThat( all.contains( ws ), equalTo( true ) );
    }

}
