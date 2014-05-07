package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;

public class JungGraphConnectionFactory
    implements RelationshipGraphConnectionFactory
{

    private final Map<String, JungGraphConnection> connections = new HashMap<String, JungGraphConnection>();

    @Override
    public RelationshipGraphConnection openConnection( final String workspaceId, final boolean create )
        throws RelationshipGraphConnectionException
    {
        JungGraphConnection connection = connections.get( workspaceId );
        if ( connection == null && create )
        {
            connection = new JungGraphConnection( workspaceId );
            connections.put( workspaceId, connection );
        }

        return connection;
    }

    @Override
    public Set<String> listWorkspaces()
    {
        return connections.keySet();
    }

    @Override
    public void flush( final RelationshipGraphConnection connection )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public boolean delete( final String workspaceId )
        throws RelationshipGraphConnectionException
    {
        return connections.remove( workspaceId ) != null;
    }

    @Override
    public void close()
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public boolean exists( final String workspaceId )
    {
        return connections.containsKey( workspaceId );
    }

}
