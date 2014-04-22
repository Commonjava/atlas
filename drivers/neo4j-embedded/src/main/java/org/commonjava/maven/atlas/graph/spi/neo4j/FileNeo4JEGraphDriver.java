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

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class FileNeo4JEGraphDriver
    extends AbstractNeo4JEGraphDriver
{

    //    private final Logger logger = new Logger( getClass() );

    public FileNeo4JEGraphDriver( final GraphWorkspaceConfiguration config, final File dbPath )
    {
        this( config, dbPath, true );
    }

    public FileNeo4JEGraphDriver( final GraphWorkspaceConfiguration config, final File dbPath, final boolean useShutdownHook )
    {
        super( config, new GraphDatabaseFactory().newEmbeddedDatabase( dbPath.getAbsolutePath() ), useShutdownHook );
    }

    public FileNeo4JEGraphDriver( final File dbPath, final boolean useShutdownHook )
    {
        super( new GraphDatabaseFactory().newEmbeddedDatabase( dbPath.getAbsolutePath() ), useShutdownHook );
    }

    //    private FileNeo4JEGraphDriver( final FileNeo4JEGraphDriver driver, final NeoGraphSession session,
    //                                   final ProjectRelationshipFilter filter, final ProjectVersionRef... refs )
    //        throws GraphDriverException
    //    {
    //        super( driver, session, filter, refs );
    //    }
    //
    //    @Override
    //    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectRelationshipFilter filter,
    //                                         final ProjectVersionRef... refs )
    //        throws GraphDriverException
    //    {
    //        final FileNeo4JEGraphDriver driver =
    //            new FileNeo4JEGraphDriver( this, getNeoSession( net.getSession() ), filter, refs );
    //        return driver;
    //    }
    //
    //    private NeoGraphSession getNeoSession( final EGraphSession session )
    //        throws GraphDriverException
    //    {
    //        if ( !( session instanceof NeoGraphSession ) )
    //        {
    //            throw new GraphDriverException( "Session of type: %s is incompatible with driver: %s!", session.getClass()
    //                                                                                                           .getName(),
    //                                            getClass().getName() );
    //        }
    //
    //        return (NeoGraphSession) session;
    //    }
    //
    //    @Override
    //    public EGraphDriver newInstance( final EGraphSession session, final EProjectNet net,
    //                                     final ProjectRelationshipFilter filter, final ProjectVersionRef... refs )
    //        throws GraphDriverException
    //    {
    //        return new FileNeo4JEGraphDriver( this, getNeoSession( session ), filter, refs );
    //    }

}
