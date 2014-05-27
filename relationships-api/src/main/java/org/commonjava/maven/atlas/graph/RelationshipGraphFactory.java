package org.commonjava.maven.atlas.graph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RelationshipGraphFactory
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final RelationshipGraphConnectionFactory connectionManager;

    private final Map<String, ConnectionCache> connectionCaches = new HashMap<String, ConnectionCache>();

    private final Set<RelationshipGraphListenerFactory> listenerFactories =
        new HashSet<RelationshipGraphListenerFactory>();

    private boolean closed;

    private final Timer timer = new Timer( true );

    public RelationshipGraphFactory( final RelationshipGraphConnectionFactory connectionFactory,
                                     final RelationshipGraphListenerFactory... listenerFactories )
    {
        this.connectionManager = connectionFactory;
        if ( listenerFactories.length > 0 )
        {
            this.listenerFactories.addAll( Arrays.asList( listenerFactories ) );
        }
    }

    public synchronized RelationshipGraph open( final ViewParams params, final boolean create )
        throws RelationshipGraphException
    {
        checkClosed();

        final String wsid = params.getWorkspaceId();
        ConnectionCache cache = connectionCaches.get( wsid );
        if ( cache == null || !cache.isOpen() )
        {
            if ( !create )
            {
                throw new RelationshipGraphException( "No such workspace: %s", wsid );
            }

            final RelationshipGraphConnection connection =
                connectionManager.openConnection( params.getWorkspaceId(), create );

            cache = new ConnectionCache( timer, connectionCaches, connection );
            connectionCaches.put( wsid, cache );

            logger.info( "Created new connection to graph db: {}", params.getWorkspaceId() );
        }
        else
        {
            logger.info( "Reusing connection to graph db: {}", params.getWorkspaceId() );
        }

        RelationshipGraph graph = cache.getGraph( params );

        if ( graph == null )
        {
            graph = new RelationshipGraph( params, cache.getConnection() );
            graph.addListener( cache );
            cache.registerGraph( params, graph );
        }
        else
        {
            cache.addGraphOwner( params );
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

    public boolean deleteWorkspace( final String workspaceId )
        throws RelationshipGraphException
    {
        checkClosed();
        return connectionManager.delete( workspaceId );
    }

    private void checkClosed()
        throws RelationshipGraphException
    {
        if ( closed )
        {
            throw new RelationshipGraphException( "Graph factory is closed!" );
        }
    }

    public synchronized void close()
        throws RelationshipGraphException
    {
        closed = true;
        timer.cancel();

        for ( final ConnectionCache cache : connectionCaches.values() )
        {
            cache.closeNow();
        }

        connectionCaches.clear();

        connectionManager.close();
    }

    public boolean workspaceExists( final String workspaceId )
    {
        return connectionManager.exists( workspaceId );
    }

    private static final class ConnectionCache
        extends AbstractRelationshipGraphListener
        implements Iterable<RelationshipGraph>
    {
        private static final long CLOSE_WAIT_TIMEOUT = 5000;

        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private RelationshipGraphConnection connection;

        private final Map<ViewParams, RelationshipGraph> graphs = new HashMap<ViewParams, RelationshipGraph>();

        private final Map<ViewParams, Integer> graphCounter = new HashMap<ViewParams, Integer>();

        private final Timer timer;

        private TimerTask closeTimer;

        private final Map<String, ConnectionCache> mapOfCaches;

        ConnectionCache( final Timer timer, final Map<String, ConnectionCache> mapOfCaches,
                         final RelationshipGraphConnection connection )
        {
            this.timer = timer;
            this.mapOfCaches = mapOfCaches;
            this.connection = connection;
        }

        public synchronized void closeNow()
            throws RelationshipGraphConnectionException
        {
            mapOfCaches.remove( this );

            graphs.clear();

            final RelationshipGraphConnection conn = connection;
            connection = null;

            conn.close();
        }

        public synchronized void startCloseTimer()
        {
            closeTimer = new TimerTask()
            {
                @Override
                public void run()
                {
                    if ( isEmpty() )
                    {
                        try
                        {
                            closeNow();
                        }
                        catch ( final RelationshipGraphConnectionException e )
                        {
                            logger.error( "Failed to close graph connection cache: " + connection, e );
                        }
                    }
                }
            };

            logger.info( "Starting close-cache countdown for: {} ({}ms)", connection.getWorkspaceId(),
                         CLOSE_WAIT_TIMEOUT );

            timer.schedule( closeTimer, CLOSE_WAIT_TIMEOUT );
        }

        public synchronized boolean isOpen()
        {
            return connection != null;
        }

        synchronized void addGraphOwner( final ViewParams params )
        {
            Integer i = graphCounter.remove( params );
            if ( i == null )
            {
                i = Integer.valueOf( 1 );
            }
            else
            {
                i = Integer.valueOf( i.intValue() + 1 );
            }

            graphCounter.put( params, i );
        }

        RelationshipGraph getGraph( final ViewParams params )
        {
            final RelationshipGraph graph = graphs.get( params );

            logger.info( "Returning existing graph for: {}", params.getWorkspaceId() );

            if ( graph != null )
            {
                cancelCloseTimer();
            }

            return graph;
        }

        private synchronized void cancelCloseTimer()
        {
            if ( closeTimer != null )
            {
                logger.info( "Canceling close-cache countdown for: {} (was scheduled to run in: {}ms)",
                             connection.getWorkspaceId(),
                             ( closeTimer.scheduledExecutionTime() - System.currentTimeMillis() ) );

                closeTimer.cancel();
                closeTimer = null;
            }
        }

        RelationshipGraphConnection getConnection()
        {
            return connection;
        }

        synchronized void registerGraph( final ViewParams params, final RelationshipGraph graph )
        {
            addGraphOwner( params );
            logger.info( "Registering new connection to: {}", params.getWorkspaceId() );
            graphs.put( params, graph );
            cancelCloseTimer();
        }

        synchronized void deregisterGraph( final ViewParams params )
        {
            Integer i = graphCounter.remove( params );
            if ( i == null || i.intValue() < 2 )
            {
                graphs.remove( params );
                if ( isEmpty() )
                {
                    startCloseTimer();
                }
            }
            else
            {
                i = Integer.valueOf( i.intValue() - 1 );
                graphCounter.put( params, i );
            }
        }

        boolean isEmpty()
        {
            return graphs.isEmpty();
        }

        @Override
        public synchronized Iterator<RelationshipGraph> iterator()
        {
            return new HashSet<RelationshipGraph>( graphs.values() ).iterator();
        }

        @Override
        public void closed( final RelationshipGraph graph )
            throws RelationshipGraphException
        {
            // TODO: It'd be nice to be able to flush to disk without wrecking the connection for all other views on the 
            // same workspace...

            // factory.flush( cache.getConnection() );

            deregisterGraph( graph.getParams() );
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
