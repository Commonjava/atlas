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

import java.util.Arrays;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.impl.MemoryGraphDatabaseFactory;

public class MemoryNeo4JEGraphDriver
    extends AbstractNeo4JEGraphDriver
{

    //    private final Logger logger = new Logger( getClass() );

    public MemoryNeo4JEGraphDriver()
    {
        this( true );
    }

    public MemoryNeo4JEGraphDriver( final boolean useShutdownHook )
    {
        super( new MemoryGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                                               .newGraphDatabase(), useShutdownHook );
    }

    private MemoryNeo4JEGraphDriver( final MemoryNeo4JEGraphDriver driver )
    {
        super( driver );
    }

    public EGraphDriver newInstance()
        throws GraphDriverException
    {
        return new MemoryNeo4JEGraphDriver( this );
    }

    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectVersionRef... refs )
    {
        final MemoryNeo4JEGraphDriver driver = new MemoryNeo4JEGraphDriver( this );
        driver.restrictToRoots( Arrays.asList( refs ), net );

        return driver;
    }

}
