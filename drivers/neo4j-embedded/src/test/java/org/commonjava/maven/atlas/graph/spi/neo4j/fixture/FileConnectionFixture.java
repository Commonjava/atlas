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
