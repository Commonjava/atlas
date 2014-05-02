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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;

public abstract class SubGraphSelectionTCK
    extends AbstractSPI_TCK
{

    @Test
    //    @Ignore
    public void selectVersionForVariableSubgraph()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                  new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        System.out.println( "Before selection here are the variable nodes: " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final RelationshipGraph graph2 =
            graphFactory().open( new ViewParams.Builder( graph.getParams() ).withSelection( varDep.asProjectRef(),
                                                                                            selected )
                                                                            .build(), false );

        //        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );
        //        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph2.getVariableSubgraphs();
        System.out.println( "After selection here are the variable nodes: " + variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        final Set<ProjectVersionRef> incomplete = graph2.getIncompleteSubgraphs();
        System.out.println( "Checking missing subgraphs for: " + selected );
        assertThat( incomplete.contains( selected ), equalTo( true ) );
    }

    @Test
    //    @Ignore
    public void selectVersionForVariableSubgraph_SelectionsContextualToView()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                  new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        // Select a concrete version for the session associated with the FIRST graph.
        // Second graph session should remain unchanged.
        final RelationshipGraph graph2 =
            graphFactory().open( new ViewParams.Builder( graph.getParams() ).withSelection( varDep.asProjectRef(),
                                                                                            selected )
                                                                            .build(), false );

        assertThat( graph.getParams()
                         .getSelection( varDep ), nullValue() );
        assertThat( graph2.getParams()
                          .getSelection( varDep ), equalTo( selected ) );

        assertThat( selected.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        assertThat( selected.asProjectVersionRef()
                            .equals( varDep.asProjectVersionRef() ), equalTo( false ) );

        variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( false ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selected ), equalTo( false ) );

        incomplete = graph2.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selected ), equalTo( true ) );

    }

}
