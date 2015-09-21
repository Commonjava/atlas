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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileNeo4jConnectionFactory
    implements RelationshipGraphConnectionFactory
{
    private final Map<String, FileNeo4JGraphConnection> openConnections =
        new HashMap<String, FileNeo4JGraphConnection>();

    private final File dbBaseDirectory;

    private final boolean useShutdownHook;

    private int storageBatchSize = FileNeo4JGraphConnection.DEFAULT_BATCH_SIZE;

    public FileNeo4jConnectionFactory( final File dbBaseDirectory, final boolean useShutdownHook, final int storageBatchSize )
    {
        this.dbBaseDirectory = dbBaseDirectory;
        this.useShutdownHook = useShutdownHook;
        this.storageBatchSize = storageBatchSize;
    }

    public FileNeo4jConnectionFactory( final File dbBaseDirectory, final boolean useShutdownHook )
    {
        this.dbBaseDirectory = dbBaseDirectory;
        this.useShutdownHook = useShutdownHook;
    }

    @Override
    public synchronized RelationshipGraphConnection openConnection( final String workspaceId, final boolean create )
        throws RelationshipGraphConnectionException
    {
        final File db = new File( dbBaseDirectory, workspaceId );
        if ( !db.exists() )
        {
            if ( !create )
            {
                throw new RelationshipGraphConnectionException( "Workspace does not exist: %s.", workspaceId );
            }
            else if ( !db.mkdirs() )
            {
                throw new RelationshipGraphConnectionException(
                                                                "Failed to create workspace directory for: %s. (dir: %s)",
                                                                workspaceId, db );
            }
            //
            //            try
            //            {
            //                Thread.sleep( 20 );
            //            }
            //            catch ( final InterruptedException e )
            //            {
            //                Thread.currentThread()
            //                      .interrupt();
            //                return null;
            //            }
        }

        FileNeo4JGraphConnection conn = openConnections.get( workspaceId );
        if ( conn == null || !conn.isOpen() )
        {
            conn = new FileNeo4JGraphConnection( workspaceId, db, useShutdownHook, storageBatchSize, this );
            openConnections.put( workspaceId, conn );
        }

        return conn;
    }

    @Override
    public Set<String> listWorkspaces()
    {
        if( !dbBaseDirectory.exists() )
        {
            return Collections.emptySet();
        }

        String[] listing = dbBaseDirectory.list();
        if ( listing == null )
        {
            return Collections.emptySet();
        }

        return new HashSet<String>( Arrays.asList( listing ) );
    }

    @Override
    public void flush( final RelationshipGraphConnection connection )
        throws RelationshipGraphConnectionException
    {
        // TODO How do I flush the graph to disk while other views may be modifying it??
    }

    @Override
    public boolean delete( final String workspaceId )
        throws RelationshipGraphConnectionException
    {
        final File db = new File( dbBaseDirectory, workspaceId );
        if ( !db.exists() || !db.isDirectory() )
        {
            return false;
        }

        try
        {
            final FileNeo4JGraphConnection connection = openConnections.remove( workspaceId );
            if ( connection != null )
            {
                connection.close();
            }

            FileUtils.forceDelete( db );
            return !db.exists();
        }
        catch ( final IOException e )
        {
            throw new RelationshipGraphConnectionException( "Failed to delete: %s. Reason: %s", e, db, e.getMessage() );
        }
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );

        final Set<String> failedClose = new HashSet<String>();
        for ( final FileNeo4JGraphConnection conn : new HashSet<FileNeo4JGraphConnection>( openConnections.values() ) )
        {
            try
            {
                conn.close();
            }
            catch ( final IOException e )
            {
                failedClose.add( conn.getWorkspaceId() );
                logger.error( "Failed to close: " + conn.getWorkspaceId() + ".", e );
            }
        }

        openConnections.clear();

        if ( !failedClose.isEmpty() )
        {
            throw new IOException( "Failed to close: " + StringUtils.join( failedClose, ", " ) );
        }
    }

    @Override
    public boolean exists( final String workspaceId )
    {
        final File db = new File( dbBaseDirectory, workspaceId );
        return db.exists() && db.isDirectory();
    }

    public synchronized void connectionClosing( final String workspaceId )
    {
        openConnections.remove( workspaceId );
    }

}
