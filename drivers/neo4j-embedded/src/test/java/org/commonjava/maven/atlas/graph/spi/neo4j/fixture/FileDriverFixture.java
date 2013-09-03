/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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

import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.commonjava.util.logging.Logger;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public class FileDriverFixture
    extends ExternalResource
{

    private final Logger logger = new Logger( getClass() );

    private final TemporaryFolder folder = new TemporaryFolder();

    private FileNeo4jWorkspaceFactory factory;

    @Override
    protected void after()
    {
        super.after();
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

        logger.info( "Initializing db in: %s", dbDir );
        factory = new FileNeo4jWorkspaceFactory( dbDir, true );
    }

    public GraphWorkspaceFactory factory()
    {
        return factory;
    }
}
