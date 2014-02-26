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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNeo4jWorkspaceFactory
    implements GraphWorkspaceFactory
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final File dbBaseDirectory;

    private final boolean useShutdownHook;

    private final JsonSerializer serializer = new JsonSerializer();

    public FileNeo4jWorkspaceFactory( final File dbBaseDirectory, final boolean useShutdownHook )
    {
        this.dbBaseDirectory = dbBaseDirectory;
        this.useShutdownHook = useShutdownHook;
    }

    @Override
    public boolean deleteWorkspace( final String id )
        throws IOException
    {
        final File db = new File( dbBaseDirectory, id );
        if ( !db.exists() || !db.isDirectory() )
        {
            return false;
        }

        FileUtils.forceDelete( db );

        return true;
    }

    @Override
    public synchronized GraphWorkspace createWorkspace( final String id, final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final File db = new File( dbBaseDirectory, id );
        if ( db.exists() )
        {
            throw new GraphDriverException( "Workspace directory already exists: {}. Cannot create workspace.", id );
        }
        else if ( !db.mkdirs() )
        {
            throw new GraphDriverException( "Failed to create workspace directory for: {}. (dir: {})", id, db );
        }

        final GraphWorkspace ws = new GraphWorkspace( id, config, new FileNeo4JEGraphDriver( db, useShutdownHook ) );
        storeWorkspace( ws );

        return ws;
    }

    @Override
    public synchronized GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        try
        {
            Thread.sleep( 20 );
        }
        catch ( final InterruptedException e )
        {
            return null;
        }

        final String id = Long.toString( System.currentTimeMillis() );
        final File db = new File( dbBaseDirectory, id );
        if ( db.exists() || !db.mkdirs() )
        {
            throw new GraphDriverException( "Cannot create database directory for workspace: {}", id );
        }

        final GraphWorkspace ws = new GraphWorkspace( id, config, new FileNeo4JEGraphDriver( db, useShutdownHook ) );
        storeWorkspace( ws );

        return ws;
    }

    @Override
    public void storeWorkspace( final GraphWorkspace workspace )
        throws GraphDriverException
    {
        final String id = workspace.getId();
        final File db = new File( dbBaseDirectory, id );
        if ( !db.isDirectory() )
        {
            throw new GraphDriverException( "No database for workspace: {}", id );
        }

        final File configFile = new File( db, "workspace-config.json" );
        Writer out = null;
        try
        {
            out = new OutputStreamWriter( new FileOutputStream( configFile ), "UTF-8" );
            out.write( serializer.toString( workspace.getConfiguration() ) );
        }
        catch ( final IOException e )
        {
            throw new GraphDriverException( "Failed to write workspace config to: {}. Reason: {}", e, configFile, e.getMessage() );
        }
        finally
        {
            closeQuietly( out );
        }
    }

    @Override
    public GraphWorkspace loadWorkspace( final String id )
        throws GraphDriverException
    {
        final File db = new File( dbBaseDirectory, id );
        if ( !db.isDirectory() )
        {
            return null;
        }

        GraphWorkspaceConfiguration config = null;

        final File configFile = new File( db, "workspace-config.json" );
        if ( configFile.exists() )
        {
            InputStream stream = null;
            try
            {
                stream = new FileInputStream( configFile );
                config = serializer.fromStream( stream, "UTF-8", GraphWorkspaceConfiguration.class );
            }
            catch ( final IOException e )
            {
                throw new GraphDriverException( "Cannot load workspace configuration: {}. Reason: {}", e, configFile, e.getMessage() );
            }
            finally
            {
                closeQuietly( stream );
            }
        }

        if ( config == null )
        {
            throw new GraphDriverException( "No configuration found for workspace: {}. Cannot load.", id );
        }

        return new GraphWorkspace( id, config, new FileNeo4JEGraphDriver( db, useShutdownHook ), configFile.lastModified() );
    }

    @Override
    public Set<GraphWorkspace> loadAllWorkspaces( final Set<String> excludedIds )
    {
        final String[] ids = dbBaseDirectory.list();
        final Set<GraphWorkspace> results = new HashSet<GraphWorkspace>();
        for ( final String id : ids )
        {
            if ( id.charAt( 0 ) == '.' )
            {
                continue;
            }

            if ( excludedIds.contains( id ) )
            {
                logger.info( "Skip loading workspace: {}. It's already cached in a higher layer.", id );
                continue;
            }

            try
            {
                logger.info( "Loading workspace: {}", id );
                results.add( loadWorkspace( id ) );
            }
            catch ( final GraphDriverException e )
            {
                logger.error( "Failed to load workspace: {}. Reason: {}", e, id, e.getMessage() );
            }
        }

        return results;
    }

}
