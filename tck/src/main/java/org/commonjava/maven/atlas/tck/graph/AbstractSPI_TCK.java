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
package org.commonjava.maven.atlas.tck.graph;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSPI_TCK
{

    @Rule
    public TestName naming = new TestName();

    protected URI sourceURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    protected RelationshipGraph simpleGraph( final ProjectVersionRef... roots )
        throws Exception
    {
        final ViewParams params = new ViewParams( newWorkspaceId(), roots );
        params.addActiveSource( sourceURI() );

        return graphFactory().open( params, true );
    }

    protected String newWorkspaceId()
    {
        return "Test-" + System.currentTimeMillis();
    }

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    protected abstract RelationshipGraphConnectionFactory connectionFactory()
        throws Exception;

    private RelationshipGraphFactory graphFactory;

    protected final RelationshipGraphFactory graphFactory()
        throws Exception
    {
        if ( graphFactory == null )
        {
            graphFactory = new RelationshipGraphFactory( connectionFactory() );
        }

        return graphFactory;
    }

    protected final RelationshipGraph openGraph( final ViewParams params, final boolean create )
        throws Exception
    {
        final RelationshipGraph graph =
            graphFactory().open( new ViewParams.Builder( params ).withActiveSources( Collections.singleton( RelationshipUtils.ANY_SOURCE_URI ) )
                                                                 .build(), create );
        return graph;
    }

    private long start;

    @Before
    public void printStart()
    {
        start = System.currentTimeMillis();
        System.out.printf( "***START [%s#%s] (%s)\n\n", naming.getClass(), naming.getMethodName(), new Date().toString() );
    }

    @After
    public void printEnd()
        throws Exception
    {
        System.out.printf( "\n\n***END [%s#%s] - %sms (%s)\n", naming.getClass(), naming.getMethodName(), ( System.currentTimeMillis() - start ),
                           new Date().toString() );

        if ( graphFactory != null )
        {
            graphFactory.close();
        }

        connectionFactory().close();
    }

}
