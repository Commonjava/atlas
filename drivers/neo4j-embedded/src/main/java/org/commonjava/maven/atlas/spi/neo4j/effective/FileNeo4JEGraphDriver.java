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
package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.io.File;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class FileNeo4JEGraphDriver
    extends AbstractNeo4JEGraphDriver
{

    //    private final Logger logger = new Logger( getClass() );

    public FileNeo4JEGraphDriver( final File dbPath )
    {
        this( dbPath, true );
    }

    public FileNeo4JEGraphDriver( final File dbPath, final boolean useShutdownHook )
    {
        super( new GraphDatabaseFactory().newEmbeddedDatabase( dbPath.getAbsolutePath() ), useShutdownHook );
    }

    private FileNeo4JEGraphDriver( final FileNeo4JEGraphDriver driver, final ProjectRelationshipFilter filter,
                                   final ProjectVersionRef... refs )
        throws GraphDriverException
    {
        super( driver, filter, refs );
    }

    public EGraphDriver newInstance()
        throws GraphDriverException
    {
        return new FileNeo4JEGraphDriver( this, null );
    }

    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectRelationshipFilter filter,
                                         final ProjectVersionRef... refs )
        throws GraphDriverException
    {
        final FileNeo4JEGraphDriver driver = new FileNeo4JEGraphDriver( this, filter, refs );
        return driver;
    }

}
