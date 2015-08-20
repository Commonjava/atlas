/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            final RelationshipGraphConnection connection =
                connectionManager.openConnection( params.getWorkspaceId(), create );

            cache = new ConnectionCache( timer, connectionCaches, connection, wsid );
            connectionCaches.put( wsid, cache );

            //            logger.info( "Created new connection to graph db: {}\nVia:\n  {}", params.getWorkspaceId(),
            //                         join( Thread.currentThread()
            //                                     .getStackTrace(), "\n  " ) );
        }
        //        else
        //        {
        //            logger.info( "Reusing connection to graph db: {}\nVia:\n  {}", params.getWorkspaceId(),
        //                         join( Thread.currentThread()
        //                                     .getStackTrace(), "\n  " ) );
        //        }

        RelationshipGraph graph = cache.getGraph( params );

        if ( graph == null )
        {
            graph = new RelationshipGraph( params, cache.getConnection() );
            graph.addListener( cache );
            cache.registerGraph( params, graph );
        }
        else
        {
            graph.incrementGraphOwnership();
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
        boolean result;
        try
        {
            result = connectionManager.delete( workspaceId );
        }
        finally
        {
            final ConnectionCache connectionCache = connectionCaches.get( workspaceId );
            if ( connectionCache != null )
            {
                try
                {
                    connectionCache.closeNow();
                }
                catch ( final RelationshipGraphConnectionException ex )
                {
                    logger.error( "Error when trying to close connection cache: {}", ex );
                }
                catch ( final RuntimeException ex )
                {
                    logger.error( "Unexpected error when trying to close connection cache: {}", ex );
                }
            }
        }
        return result;
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

        private final String wsid;
        
        private final Map<ViewParams, RelationshipGraph> graphs = new HashMap<ViewParams, RelationshipGraph>();

        private final Timer timer;

        private TimerTask closeTimer;

        private final Map<String, ConnectionCache> mapOfCaches;

        ConnectionCache( final Timer timer, final Map<String, ConnectionCache> mapOfCaches,
                         final RelationshipGraphConnection connection, final String wsid )
        {
            this.timer = timer;
            this.mapOfCaches = mapOfCaches;
            this.connection = connection;
            this.wsid = wsid;
        }

        public synchronized void closeNow()
            throws RelationshipGraphConnectionException
        {
            mapOfCaches.remove( wsid );

            graphs.clear();

            final RelationshipGraphConnection conn = connection;
            connection = null;

            if ( conn != null && !conn.isClosed() )
            {
                conn.close();
            }
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
            logger.info( "Registering new connection to: {}", params.getWorkspaceId() );
            graphs.put( params, graph );
            cancelCloseTimer();
        }

        synchronized void deregisterGraph( final ViewParams params )
        {
            graphs.remove( params );
            if ( isEmpty() )
            {
                startCloseTimer();
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
