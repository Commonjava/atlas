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

        assertThat( ws, notNullValue() );

        logger.info( "Created workspace: %s", ws );

        final GraphWorkspace result = getManager().getWorkspace( ws.getId() );

        logger.info( "Retrieved all workspaces: %s", result );

        assertThat( result, notNullValue() );
        assertThat( result.getId(), equalTo( ws.getId() ) );
        assertThat( result.equals( ws ), equalTo( true ) );
    }

    @Test
    public void createWorkspaceAndFindInAllWorkspacesListing()
        throws Exception
    {
        final GraphWorkspace ws = getManager().createWorkspace( new GraphWorkspaceConfiguration() );

        assertThat( ws, notNullValue() );

        logger.info( "Created workspace: %s", ws );

        final Set<GraphWorkspace> all = getManager().getAllWorkspaces();

        logger.info( "Retrieved all workspaces: %s", all );

        assertThat( all, notNullValue() );
        assertThat( all.size(), equalTo( 1 ) );
        assertThat( all.contains( ws ), equalTo( true ) );
    }

}
