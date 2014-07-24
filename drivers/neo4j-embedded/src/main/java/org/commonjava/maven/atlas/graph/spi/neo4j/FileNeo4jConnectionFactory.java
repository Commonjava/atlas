package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNeo4jConnectionFactory
    implements RelationshipGraphConnectionFactory
{

    private final Map<String, FileNeo4JGraphConnection> openConnections =
        new HashMap<String, FileNeo4JGraphConnection>();

    private final File dbBaseDirectory;

    private final boolean useShutdownHook;

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
                throw new RelationshipGraphConnectionException( "Workspace does not exist: {}.", workspaceId );
            }
            else if ( !db.mkdirs() )
            {
                throw new RelationshipGraphConnectionException(
                                                                "Failed to create workspace directory for: {}. (dir: {})",
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
            conn = new FileNeo4JGraphConnection( workspaceId, db, useShutdownHook, this );
            openConnections.put( workspaceId, conn );
        }

        return conn;
    }

    @Override
    public Set<String> listWorkspaces()
    {
        return new HashSet<String>( Arrays.asList( dbBaseDirectory.list() ) );
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

        boolean result = false;
        try
        {
            FileUtils.forceDelete( db );
            result = !db.exists();
            if ( result )
            {
                FileNeo4JGraphConnection connection = openConnections.remove( workspaceId );
                if ( connection != null )
                {
                    connection.close();
                }
            }
        }
        catch ( final IOException e )
        {
            throw new RelationshipGraphConnectionException( "Failed to delete: %s. Reason: %s", e, db, e.getMessage() );
        }

        return result;
    }

    @Override
    public synchronized void close()
        throws RelationshipGraphConnectionException
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );

        final Set<String> failedClose = new HashSet<String>();
        for ( final FileNeo4JGraphConnection conn : new HashSet<FileNeo4JGraphConnection>( openConnections.values() ) )
        {
            try
            {
                conn.close();
            }
            catch ( final RelationshipGraphConnectionException e )
            {
                failedClose.add( conn.getWorkspaceId() );
                logger.error( "Failed to close: " + conn.getWorkspaceId() + ".", e );
            }
        }

        openConnections.clear();

        if ( !failedClose.isEmpty() )
        {
            throw new RelationshipGraphConnectionException( "Failed to close: {}", new JoinString( ", ", failedClose ) );
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
