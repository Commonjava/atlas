package org.commonjava.maven.atlas.graph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;

public final class RelationshipGraphFactory
{

    private final RelationshipGraphConnectionFactory connectionManager;

    private final Map<String, ConnectionCache> connectionCaches = new HashMap<String, ConnectionCache>();

    private final Set<RelationshipGraphListenerFactory> listenerFactories =
        new HashSet<RelationshipGraphListenerFactory>();

    private boolean closed;

    public RelationshipGraphFactory( final RelationshipGraphConnectionFactory connectionFactory,
                                     final RelationshipGraphListenerFactory... listenerFactories )
    {
        this.connectionManager = connectionFactory;
        if ( listenerFactories.length > 0 )
        {
            this.listenerFactories.addAll( Arrays.asList( listenerFactories ) );
        }
    }

    public RelationshipGraph open( final ViewParams params, final boolean create )
        throws RelationshipGraphException
    {
        checkClosed();

        final String wsid = params.getWorkspaceId();
        ConnectionCache cache = connectionCaches.get( wsid );
        if ( cache == null )
        {
            if ( !create )
            {
                throw new RelationshipGraphException( "No such workspace: %s", wsid );
            }

            final RelationshipGraphConnection connection =
                connectionManager.openConnection( params.getWorkspaceId(), create );

            cache = new ConnectionCache( connection );
            connectionCaches.put( wsid, cache );
        }

        RelationshipGraph graph = cache.getGraph( params );

        if ( graph == null )
        {
            graph = new RelationshipGraph( params, cache.getConnection() );
            graph.addListener( cache );
            cache.registerGraph( params, graph );
        }

        return graph;
    }

    public Set<String> listWorkspaces()
    {
        return connectionManager.listWorkspaces();
    }

    public void store( final RelationshipGraph graph )
        throws RelationshipGraphException
    {
        checkClosed();
        connectionManager.flush( graph.getConnection() );
    }

    public void deleteWorkspace( final String workspaceId )
        throws RelationshipGraphException
    {
        checkClosed();
        connectionManager.delete( workspaceId );
    }

    private void checkClosed()
        throws RelationshipGraphException
    {
        if ( closed )
        {
            throw new RelationshipGraphException( "Graph factory is closed!" );
        }
    }

    public void close()
        throws RelationshipGraphException
    {
        closed = true;

        for ( final ConnectionCache cache : connectionCaches.values() )
        {
            cache.close();
        }

        connectionCaches.clear();

        connectionManager.close();
    }

    private static final class ConnectionCache
        extends AbstractRelationshipGraphListener
        implements Iterable<RelationshipGraph>
    {
        private final RelationshipGraphConnection connection;

        private final Map<ViewParams, RelationshipGraph> graphs = new HashMap<ViewParams, RelationshipGraph>();

        ConnectionCache( final RelationshipGraphConnection connection )
        {
            this.connection = connection;
        }

        public void close()
            throws RelationshipGraphException
        {
            graphs.clear();
            connection.close();
        }

        RelationshipGraph getGraph( final ViewParams params )
        {
            return graphs.get( params );
        }

        RelationshipGraphConnection getConnection()
        {
            return connection;
        }

        void registerGraph( final ViewParams params, final RelationshipGraph graph )
        {
            graphs.put( params, graph );
        }

        void deregisterGraph( final ViewParams params )
        {
            graphs.remove( params );
        }

        boolean isEmpty()
        {
            return graphs.isEmpty();
        }

        @Override
        public Iterator<RelationshipGraph> iterator()
        {
            return graphs.values()
                         .iterator();
        }

        @Override
        public void closed( final RelationshipGraph graph )
            throws RelationshipGraphException
        {
            // TODO: It'd be nice to be able to flush to disk without wrecking the connection for all other views on the 
            // same workspace...

            // factory.flush( cache.getConnection() );
            deregisterGraph( graph.getParams() );
            if ( isEmpty() )
            {
                close();
            }
        }

        @Override
        public int hashCode()
        {
            return connection.getWorkspaceId()
                             .hashCode() + 13;
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
            final ConnectionCache other = (ConnectionCache) obj;
            return connection.getWorkspaceId()
                             .equals( other.connection.getWorkspaceId() );
        }
    }

}
