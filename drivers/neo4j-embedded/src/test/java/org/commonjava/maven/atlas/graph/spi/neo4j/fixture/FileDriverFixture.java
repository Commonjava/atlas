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
package org.commonjava.maven.atlas.graph.spi.neo4j.fixture;

import java.io.File;
import java.io.IOException;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDriverFixture
    extends ExternalResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final TemporaryFolder folder = new TemporaryFolder();

    private FileNeo4jWorkspaceFactory factory;

    private EGraphManager manager;

    @Override
    protected void after()
    {
        super.after();
        try
        {
            manager.close();
        }
        catch ( final IOException e )
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
        factory = new FileNeo4jWorkspaceFactory( dbDir, false );
        manager = new EGraphManager( factory );
    }

    public EGraphManager manager()
    {
        return manager;
    }
}
