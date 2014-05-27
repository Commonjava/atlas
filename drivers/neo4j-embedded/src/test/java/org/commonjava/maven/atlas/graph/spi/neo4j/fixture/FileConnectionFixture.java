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
package org.commonjava.maven.atlas.graph.spi.neo4j.fixture;

import java.io.File;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileConnectionFixture
    extends ExternalResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final TemporaryFolder folder = new TemporaryFolder();

    private FileNeo4jConnectionFactory factory;

    @Override
    protected void after()
    {
        super.after();
        try
        {
            factory.close();
        }
        catch ( final RelationshipGraphConnectionException e )
        {
            e.printStackTrace();
        }

        folder.delete();
    }

    @Override
    protected void before()
        throws Throwable
    {
        folder.create();

        final File dbDir = folder.newFolder();
        dbDir.delete();
        dbDir.mkdirs();

        logger.info( "Initializing db in: {}", dbDir );
        factory = new FileNeo4jConnectionFactory( dbDir, false );
    }

    public FileNeo4jConnectionFactory connectionFactory()
    {
        return factory;
    }
}