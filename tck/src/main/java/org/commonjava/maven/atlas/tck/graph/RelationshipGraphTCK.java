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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public abstract class RelationshipGraphTCK
    extends AbstractSPI_TCK
{

    @Test
    public void createPath_ReturnNullWhenTargetVersionIsAnExpression()
        throws Exception
    {
        final ProjectVersionRef from = new ProjectVersionRef( "org.from", "project", "1.0" );
        final ProjectVersionRef to = new ProjectVersionRef( "org.to", "artifact", "${version.target}" );

        final URI src = new URI( "test:source-uri" );
        final ProjectRelationship<?> rel =
            new DependencyRelationship( src, from, to.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false );

        final RelationshipGraph graph = simpleGraph( from );
        final GraphPath<?> path = graph.createPath( rel );

        assertThat( path, nullValue() );
    }

    @Test
    public void storeBOMThenVerifyBomGAVPresentInView()
        throws Exception
    {
        final URI src = sourceURI();
        final ProjectVersionRef gav = new ProjectVersionRef( "g", "a", "v" );

        final ProjectVersionRef d1 = new ProjectVersionRef( "g", "d1", "1" );
        final ProjectVersionRef d2 = new ProjectVersionRef( "g", "d2", "2" );

        final RelationshipGraph graph =
            graphFactory().open( new ViewParams( newWorkspaceId(), new DependencyFilter(),
                                                 new ManagedDependencyMutator(), gav ), true );

        /* @formatter:off */
        graph.storeRelationships(
                new ParentRelationship(src, gav),
                new DependencyRelationship(src, gav, d1.asArtifactRef("jar",
                        null), DependencyScope.compile, 0, true),
                new DependencyRelationship(src, gav, d2.asArtifactRef("jar",
                        null), DependencyScope.compile, 1, true));
        /* @formatter:on */

        graph.containsGraph( gav );
    }

    @Test
    public void connectThreeGraphsWithParentInterrelationships()
        throws Exception
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final String wsid = newWorkspaceId();

        graphFactory().open( new ViewParams( wsid, r ), true )
                      .storeRelationships( new ParentRelationship( source, r ) );

        graphFactory().open( new ViewParams( wsid, p ), true )
                      .storeRelationships( new ParentRelationship( source, p, r ) );
        
        final RelationshipGraph child = graphFactory().open( new ViewParams( wsid, c ), true );

        child.storeRelationships( new ParentRelationship( source, c, p ) );

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

    @Test
    public void connectThreeGraphsWithParentInterrelationships_WrongOrder()
        throws Exception
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final String wsid = newWorkspaceId();

        final RelationshipGraph child = graphFactory().open( new ViewParams( wsid, c ), true );

        child.storeRelationships( new ParentRelationship( source, c, p ) );

        graphFactory().open( new ViewParams( wsid, p ), true )
                      .storeRelationships( new ParentRelationship( source, p, r ) );
        
        graphFactory().open( new ViewParams( wsid, r ), true )
                      .storeRelationships( new ParentRelationship( source, r ) );
        
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
