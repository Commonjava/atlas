package org.apache.maven.graph.effective;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.traverse.AncestryTraversal;
import org.junit.Test;

public class EProjectGraphTest
{

    @Test
    public void connectThreeGraphsWithParentInterrelationships()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef r = new ProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "org.test", "child", "1.0" );

        final EProjectGraph root = new EProjectGraph.Builder( r ).build();
        final EProjectGraph parent = new EProjectGraph.Builder( p ).withParent( r )
                                                                   .build();
        final EProjectGraph child = new EProjectGraph.Builder( c ).withParent( p )
                                                                  .build();
        parent.connect( root );
        child.connect( parent );

        assertThat( child.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        child.traverse( ancestryTraversal );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

}
