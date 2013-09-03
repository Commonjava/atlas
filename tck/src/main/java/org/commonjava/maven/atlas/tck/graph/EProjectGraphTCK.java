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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;
import org.junit.Test;

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
        /* @formatter:off */
        final EProjectGraph root = getManager().createGraph( 
                session, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, r ) ).build()
        );
        
        final EProjectGraph parent = getManager().createGraph( 
                session, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, p ) )
                    .withParent( new ParentRelationship( source, p, r ) )
                    .build()
        );
        
        final EProjectGraph child = getManager().createGraph(
                session,
                new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                    .withParent( new ParentRelationship( source, c, p ) )
                    .build()
        );
        /* @formatter:on */

        System.out.println( "Incomplete subgraphs: " + child.getIncompleteSubgraphs() );
        assertThat( child.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        child.traverse( ancestryTraversal );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        new Logger( getClass() ).info( "Ancestry: %s", ancestry );

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
        new Logger( getClass() ).info( "Ancestry: %s", ancestry );

        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

}
