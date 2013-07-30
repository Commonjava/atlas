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
package org.commonjava.maven.atlas.tck.graph;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class AbstractSPI_TCK
{

    @Rule
    public TestName naming = new TestName();

    protected URI sourceURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    protected GraphWorkspace simpleSession()
        throws Exception
    {
        return getManager().createWorkspace( new GraphWorkspaceConfiguration().withSource( sourceURI() ) );
    }

    protected final Logger logger = new Logger( getClass() );

    protected abstract EGraphManager getManager()
        throws Exception;

    private long start;

    @Before
    public void printStart()
    {
        start = System.currentTimeMillis();
        System.out.printf( "***START [%s#%s] (%s)\n\n", naming.getClass(), naming.getMethodName(),
                           new Date().toString() );
    }

    @After
    public void printEnd()
    {
        System.out.printf( "\n\n***END [%s#%s] - %dms (%s)\n", naming.getClass(), naming.getMethodName(),
                           ( System.currentTimeMillis() - start ), new Date().toString() );
    }

}
