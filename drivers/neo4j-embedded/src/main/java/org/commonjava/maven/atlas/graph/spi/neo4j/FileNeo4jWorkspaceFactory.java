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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNeo4jWorkspaceFactory
    implements GraphWorkspaceFactory
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final File dbBaseDirectory;

    private final boolean useShutdownHook;

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
        throws RelationshipGraphConnectionException
    {
        final File db = new File( dbBaseDirectory, id );
        if ( db.exists() )
        {
            throw new RelationshipGraphConnectionException( "Workspace directory already exists: {}. Cannot create workspace.", id );
        }
        else if ( !db.mkdirs() )
        {
            throw new RelationshipGraphConnectionException( "Failed to create workspace directory for: {}. (dir: {})", id, db );
        }

        final GraphWorkspace ws = new GraphWorkspace( id, new FileNeo4JEGraphDriver( config, db, useShutdownHook ) );
        storeWorkspace( ws );

        return ws;
    }

    @Override
    public synchronized GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws RelationshipGraphConnectionException
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
            throw new RelationshipGraphConnectionException( "Cannot create database directory for workspace: {}", id );
        }

        final GraphWorkspace ws = new GraphWorkspace( id, new FileNeo4JEGraphDriver( config, db, useShutdownHook ) );
        storeWorkspace( ws );

        return ws;
    }

    @Override
    public void storeWorkspace( final GraphWorkspace workspace )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public GraphWorkspace loadWorkspace( final String id )
        throws RelationshipGraphConnectionException
    {
        final File db = new File( dbBaseDirectory, id );
        if ( !db.isDirectory() )
        {
            return null;
        }

        return new GraphWorkspace( id, new FileNeo4JEGraphDriver( db, useShutdownHook ) );
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
            catch ( final RelationshipGraphConnectionException e )
            {
                logger.error( String.format( "Failed to load workspace: %s. Reason: %s", id, e.getMessage() ), e );
            }
        }

        return results;
    }

}
