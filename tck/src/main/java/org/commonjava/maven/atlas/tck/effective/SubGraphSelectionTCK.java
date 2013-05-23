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
package org.commonjava.maven.atlas.tck.effective;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionUtils;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.spi.effective.EGraphDriver;
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
        final SingleVersion selected = VersionUtils.createSingleVersion( "1.0-20130314.161200-1" );

        final URI source = sourceURI();

        /* @formatter:off */
        final EProjectGraph graph =
            new EProjectGraph.Builder( new EProjectKey( source, project ), newDriverInstance() )
                .withDependencies( new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                   new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) )
                .build();
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final ProjectVersionRef selDep = graph.selectVersionFor( varDep, selected );
        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph.getVariableSubgraphs();
        assertThat( variables.isEmpty(), equalTo( true ) );

        final Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selDep ), equalTo( true ) );
    }

    @Test
    public void selectThenClearVersionForVariableSubgraph()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final SingleVersion selected = VersionUtils.createSingleVersion( "1.0-20130314.161200-1" );

        final URI source = sourceURI();

        /* @formatter:off */
        final EProjectGraph graph =
            new EProjectGraph.Builder( new EProjectKey( source, project ), newDriverInstance() )
                .withDependencies( new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                   new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) )
                .build();
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        System.out.println( "Variable before selecting:\n  " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final ProjectVersionRef selDep = graph.selectVersionFor( varDep, selected );
        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( "Variable after selecting:\n  " + variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        System.out.println( "Incomplete after selecting:\n  " + incomplete );
        assertThat( incomplete.contains( selDep ), equalTo( true ) );

        final Map<ProjectVersionRef, ProjectVersionRef> cleared = graph.clearSelectedVersions();
        System.out.println( "Cleared:\n  " + cleared );
        assertThat( cleared.get( varDep ), equalTo( selDep ) );

        variables = graph.getVariableSubgraphs();
        System.out.println( "Variable after clearing:\n  " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        incomplete = graph.getIncompleteSubgraphs();

        System.out.println( "Incomplete after clearing:\n  " + incomplete );
        assertThat( incomplete.contains( selDep ), equalTo( false ) );
        assertThat( incomplete.contains( varDep ), equalTo( false ) );
    }

    @Test
    //    @Ignore
    public void selectVersionForVariableSubgraph_SelectionsContextualToRoots()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef project2 = new ProjectVersionRef( "org.my", "project", "1.0.1" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final SingleVersion selected = VersionUtils.createSingleVersion( "1.0-20130314.161200-1" );

        final EGraphDriver rootDriver = newDriverInstance();
        final URI source = sourceURI();

        /* @formatter:off */
        final EProjectGraph graph =
            new EProjectGraph.Builder( new EProjectKey( source, project ), rootDriver )
                .withDependencies( new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                   new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false ) )
                .build();

        final EProjectGraph graph2 =
            new EProjectGraph.Builder( new EProjectKey( source, project2 ), rootDriver )
                .withDependencies( new DependencyRelationship( source, project2, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                                   new DependencyRelationship( source, varDep,   new ArtifactRef( varD2,  null, null, false ), null, 0, false ) )
                .build();
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final ProjectVersionRef selDep = graph.selectVersionFor( varDep, selected );
        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final Set<ProjectVersionRef> incomplete = graph2.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selDep ), equalTo( false ) );
    }

}
