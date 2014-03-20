/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
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
    @Ignore
    public void selectVersionForVariableSubgraph()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();
        final GraphView view = new GraphView( ws, AnyFilter.INSTANCE, new ManagedDependencyMutator(), project );

        /* @formatter:off */
        getManager().storeRelationships( ws, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                        new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        
        final EProjectGraph graph = getManager().getGraph( view );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );
        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        final Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selDep ), equalTo( true ) );
    }

    @Test
    @Ignore
    public void selectThenClearVersionForVariableSubgraph()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();
        final GraphView view = new GraphView( ws, AnyFilter.INSTANCE, new ManagedDependencyMutator(), project );

        /* @formatter:off */
        getManager().storeRelationships( ws, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                        new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        
        final EProjectGraph graph = getManager().getGraph( view );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        System.out.println( "Variable before selecting:\n  " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );
        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( "Variable after selecting:\n  " + variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        System.out.println( "Incomplete after selecting:\n  " + incomplete );
        assertThat( incomplete.contains( selDep ), equalTo( true ) );

        view.clearSelections();

        variables = graph.getVariableSubgraphs();
        System.out.println( "Variable after clearing:\n  " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        incomplete = graph.getIncompleteSubgraphs();

        System.out.println( "Incomplete after clearing:\n  " + incomplete );
        assertThat( incomplete.contains( selDep ), equalTo( false ) );
        assertThat( incomplete.contains( varDep ), equalTo( false ) );
    }

    @Test
    @Ignore
    public void selectVersionForVariableSubgraph_SelectionsContextualToSession()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final GraphWorkspace session = simpleWorkspace();
        final GraphView view = new GraphView( session, AnyFilter.INSTANCE, new ManagedDependencyMutator(), project );

        final GraphWorkspace session2 = simpleWorkspace();
        final GraphView view2 = new GraphView( session2, project );

        /* @formatter:off */
        getManager().storeRelationships( session, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                        new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        
        getManager().storeRelationships( session2, new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) );
         
        final EProjectGraph graph = getManager().getGraph( view );
        final EProjectGraph graph2 = getManager().getGraph( view2 );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        // Select a concrete version for the session associated with the FIRST graph.
        // Second graph session should remain unchanged.
        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );

        assertThat( view.getSelection( varDep ), equalTo( selected ) );
        assertThat( view2.getSelection( varDep ), nullValue() );

        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        assertThat( selDep.asProjectVersionRef()
                          .equals( varDep.asProjectVersionRef() ), equalTo( false ) );

        variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( false ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selDep ), equalTo( true ) );

        incomplete = graph2.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selDep ), equalTo( false ) );
    }

}
