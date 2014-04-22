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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public abstract class EProjectGraphTCK
    extends AbstractSPI_TCK
{

    @SuppressWarnings( "unused" )
    @Test
    public void connectThreeGraphsWithParentInterrelationships()
        throws Exception
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final GraphWorkspace session = simpleWorkspace();
        final GraphView view = new GraphView( session, c );

        /* @formatter:off */
        final EProjectGraph root = getManager().createGraph( 
                view, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, r ) ).build()
        );
        
        final EProjectGraph parent = getManager().createGraph( 
                view, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, p ) )
                    .withParent( new ParentRelationship( source, p, r ) )
                    .build()
        );
        
        final EProjectGraph child = getManager().createGraph(
                view,
                new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                    .withParent( new ParentRelationship( source, c, p ) )
                    .build()
        );
        /* @formatter:on */

        System.out.println( "Incomplete subgraphs: " + child.getIncompleteSubgraphs() );
        System.out.flush();
        assertThat( child.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        child.traverse( ancestryTraversal );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        LoggerFactory.getLogger( getClass() )
                     .info( "Ancestry: {}", ancestry );

        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

    @SuppressWarnings( "unused" )
    @Test
    public void connectThreeGraphsWithParentInterrelationships_WrongOrder()
        throws Exception
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final GraphWorkspace session = simpleWorkspace();
        /* @formatter:off */
        final EProjectGraph child = getManager().createGraph(
                session,
                new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                    .withParent( new ParentRelationship( source, c, p ) )
                    .build()
        );
        
        final EProjectGraph parent = getManager().createGraph( 
                session, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, p ) )
                    .withParent( new ParentRelationship( source, p, r ) )
                    .build()
        );
        
        final EProjectGraph root = getManager().createGraph( 
                session, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, r ) ).build()
        );
        /* @formatter:on */

        assertThat( child.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        child.traverse( ancestryTraversal );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        LoggerFactory.getLogger( getClass() )
                     .info( "Ancestry: {}", ancestry );

        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

}
