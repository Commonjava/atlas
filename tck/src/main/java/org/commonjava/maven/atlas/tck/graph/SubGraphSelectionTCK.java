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
import java.util.Collections;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Ignore;
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
        final GraphWorkspace ws = simpleGraph();
        GraphView view = new GraphView( ws, AnyFilter.INSTANCE, new ManagedDependencyMutator(), project );

        /* @formatter:off */
        graphFactory().storeRelationships( ws, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                        new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        
        EProjectGraph graph = graphFactory().getGraph( view );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        System.out.println( "Before selection in view: " + view.getShortId() + ", here are the variable nodes: " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        view = new GraphView( ws, AnyFilter.INSTANCE, view.getMutator(), Collections.singletonMap( varDep.asProjectRef(), selected ), project );
        graph = graphFactory().getGraph( view );

        //        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );
        //        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( "After selection in view: " + view.getShortId() + ", here are the variable nodes: " + variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        final Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        System.out.println( "Checking missing subgraphs for: " + selected );
        assertThat( incomplete.contains( selected ), equalTo( true ) );
    }

    @Test
    @Ignore( value = "Selection clearing is not currently supported" )
    public void selectThenClearVersionForVariableSubgraph()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleGraph();
        GraphView view = new GraphView( ws, AnyFilter.INSTANCE, new ManagedDependencyMutator(), project );

        /* @formatter:off */
        graphFactory().storeRelationships( ws, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                        new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        
        EProjectGraph graph = graphFactory().getGraph( view );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        System.out.println( "Variable before selecting:\n  " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        view = new GraphView( ws, AnyFilter.INSTANCE, view.getMutator(), Collections.singletonMap( varDep.asProjectRef(), selected ), project );
        graph = graphFactory().getGraph( view );

        //        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );
        //        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( "Variable after selecting:\n  " + variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        System.out.println( "Incomplete after selecting:\n  " + incomplete );
        assertThat( incomplete.contains( selected ), equalTo( true ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( "Variable after clearing:\n  " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        incomplete = graph.getIncompleteSubgraphs();

        System.out.println( "Incomplete after clearing:\n  " + incomplete );
        assertThat( incomplete.contains( selected ), equalTo( false ) );
        assertThat( incomplete.contains( varDep ), equalTo( false ) );
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
        final GraphWorkspace session = simpleGraph();
        GraphView view = new GraphView( session, AnyFilter.INSTANCE, new ManagedDependencyMutator(), project );

        final GraphWorkspace session2 = simpleGraph();
        final GraphView view2 = new GraphView( session2, project );

        /* @formatter:off */
        graphFactory().storeRelationships( session, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                        new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        
        graphFactory().storeRelationships( session2, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
         
        EProjectGraph graph = graphFactory().getGraph( view );
        final EProjectGraph graph2 = graphFactory().getGraph( view2 );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        // Select a concrete version for the session associated with the FIRST graph.
        // Second graph session should remain unchanged.
        view = new GraphView( session, AnyFilter.INSTANCE, view.getMutator(), Collections.singletonMap( varDep.asProjectRef(), selected ), project );
        graph = graphFactory().getGraph( view );

        assertThat( view.getSelection( varDep ), equalTo( selected ) );
        assertThat( view2.getSelection( varDep ), nullValue() );

        assertThat( selected.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        assertThat( selected.asProjectVersionRef()
                            .equals( varDep.asProjectVersionRef() ), equalTo( false ) );

        variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( false ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selected ), equalTo( true ) );

        incomplete = graph2.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selected ), equalTo( false ) );
    }

}
