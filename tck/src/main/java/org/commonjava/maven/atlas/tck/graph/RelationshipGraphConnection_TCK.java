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

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.junit.Test;

public abstract class RelationshipGraphConnection_TCK
    extends AbstractSPI_TCK
{

    @Test
    public void createWorkspaceAndRetrieveById()
        throws Exception
    {
        final String wsid = newWorkspaceId();
        final RelationshipGraphConnection connection = connectionFactory().openConnection( wsid, true );

        System.out.println( "wsid: " + connection.getWorkspaceId() );

        assertThat( connection, notNullValue() );

        logger.info( "Created connection: {}", connection );

        final RelationshipGraphConnection result = connectionFactory().openConnection( wsid, false );

        logger.info( "Retrieved connection: {}", result );

        assertThat( result, notNullValue() );
        assertThat( result.getWorkspaceId(), equalTo( connection.getWorkspaceId() ) );
        assertThat( result.equals( connection ), equalTo( true ) );
    }

    @Test
    public void createWorkspaceAndFindInAllWorkspacesListing()
        throws Exception
    {
        final String wsid = newWorkspaceId();
        final RelationshipGraphConnection connection = connectionFactory().openConnection( wsid, true );

        System.out.println( "wsid: " + connection.getWorkspaceId() );

        assertThat( connection, notNullValue() );

        logger.info( "Created connection: {}", connection );

        final Set<String> all = connectionFactory().listWorkspaces();

        logger.info( "Retrieved all workspaces: {}", all );

        assertThat( all, notNullValue() );
        assertThat( all.size(), equalTo( 1 ) );
        assertThat( all.contains( connection.getWorkspaceId() ), equalTo( true ) );
    }

}
