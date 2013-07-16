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

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.common.ref.ArtifactRef;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.EProjectCycle;
import org.commonjava.maven.atlas.effective.EProjectGraph;
import org.commonjava.maven.atlas.effective.filter.DependencyFilter;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.PluginRelationship;
import org.commonjava.maven.atlas.effective.workspace.GraphWorkspace;
import org.junit.Test;

public abstract class CycleDetectionTCK
    extends AbstractSPI_TCK
{

    @Test
    //    @Ignore
    public void introducesCycleCheckWithExistingGraph()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef dep = new ProjectVersionRef( "org.other", "dep", "1.0" );
        final ProjectVersionRef dep2 = new ProjectVersionRef( "org.other", "dep2", "1.0" );

        /* @formatter:off */
        getManager().storeRelationships( new DependencyRelationship( source, project, new ArtifactRef( dep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep,  new ArtifactRef( dep2,  null, null, false ), null, 0, false ) );
        
        final GraphWorkspace session = simpleSession();
        final EProjectGraph graph = getManager().getGraph( session, project );

        final boolean introduces = graph.introducesCycle( new DependencyRelationship( source, dep,  new ArtifactRef( project,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        assertThat( introduces, equalTo( true ) );
    }

    @Test
    //    @Ignore
    public void buildGraphWithCycleBackToRootAndRetrieveCycle()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef dep = new ProjectVersionRef( "org.other", "dep", "1.0" );
        final ProjectVersionRef dep2 = new ProjectVersionRef( "org.other", "dep2", "1.0" );

        /* @formatter:off */
        getManager().storeRelationships( new DependencyRelationship( source, project, new ArtifactRef( dep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep,  new ArtifactRef( dep2,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep2,  new ArtifactRef( project,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final GraphWorkspace session = simpleSession();
        final EProjectGraph graph = getManager().getGraph( session, project );

        final Set<EProjectCycle> cycles = graph.getCycles();
        System.out.println( "Cycles:\n\n" + join( cycles, "\n" ) );
        assertThat( cycles.size(), equalTo( 1 ) );

        for ( final EProjectCycle cycle : cycles )
        {
            final Set<ProjectVersionRef> projects = cycle.getAllParticipatingProjects();
            assertThat( projects.contains( project ), equalTo( true ) );
            assertThat( projects.contains( dep ), equalTo( true ) );
            assertThat( projects.contains( dep2 ), equalTo( true ) );
        }
    }

    @Test
    //    @Ignore
    public void buildGraphWithCycleBetweenDepLevelsAndRetrieveCycle()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef dep = new ProjectVersionRef( "org.other", "dep", "1.0" );
        final ProjectVersionRef dep2 = new ProjectVersionRef( "org.other", "dep2", "1.0" );

        /* @formatter:off */
        getManager().storeRelationships( new DependencyRelationship( source, project, new ArtifactRef( dep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep,  new ArtifactRef( dep2,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep2,  new ArtifactRef( dep,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final GraphWorkspace session = simpleSession();
        final EProjectGraph graph = getManager().getGraph( session, project );

        final Set<EProjectCycle> cycles = graph.getCycles();
        System.out.println( "Cycles:\n\n" + join( cycles, "\n" ) );
        assertThat( cycles.size(), equalTo( 1 ) );

        for ( final EProjectCycle cycle : cycles )
        {
            final Set<ProjectVersionRef> projects = cycle.getAllParticipatingProjects();
            assertThat( projects.contains( project ), equalTo( false ) );
            assertThat( projects.contains( dep ), equalTo( true ) );
            assertThat( projects.contains( dep2 ), equalTo( true ) );
        }
    }

    @Test
    //    @Ignore
    public void GB_cycleFromGraph1PresentInGraph2WhenNodeIsCrossReferenced()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef a = new ProjectVersionRef( "project", "A", "1.0" );
        final ProjectVersionRef b = new ProjectVersionRef( "project", "B", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "project", "C", "1.0" );

        final ProjectVersionRef d = new ProjectVersionRef( "project", "D", "1.0" );
        final ProjectVersionRef e = new ProjectVersionRef( "project", "E", "1.0" );

        /* @formatter:off */
        // a --> b --> c --> a
        // d --> e --> c --> a --> b --> c
        getManager().storeRelationships( new DependencyRelationship( source, a, new ArtifactRef( b, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, b,  new ArtifactRef( c,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, c,  new ArtifactRef( a,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, d, new ArtifactRef( e, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, e,  new ArtifactRef( c,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final GraphWorkspace session = simpleSession();
        final EProjectGraph graph1 = getManager().getGraph( session, a );
        final EProjectGraph graph2 = getManager().getGraph( session, d );

        final Set<EProjectCycle> cycles1 = graph1.getCycles();
        System.out.println( "Graph 1 Cycles:\n\n" + join( cycles1, "\n" ) );

        final Set<EProjectCycle> cycles2 = graph2.getCycles();
        System.out.println( "Graph 2 Cycles:\n\n" + join( cycles2, "\n" ) );

        assertThat( cycles1.size(), equalTo( 1 ) );
        assertThat( cycles2.size(), equalTo( 1 ) );

        final Set<Set<EProjectCycle>> cycleSets = new HashSet<Set<EProjectCycle>>();
        cycleSets.add( cycles1 );
        cycleSets.add( cycles2 );

        int i = 0;
        for ( final Set<EProjectCycle> cycles : cycleSets )
        {
            int j = 0;
            for ( final EProjectCycle cycle : cycles )
            {
                final Set<ProjectVersionRef> refs = cycle.getAllParticipatingProjects();
                assertThat( i + ", " + j + " missing A", refs.contains( a ), equalTo( true ) );
                assertThat( i + ", " + j + " missing B", refs.contains( b ), equalTo( true ) );
                assertThat( i + ", " + j + " missing C", refs.contains( c ), equalTo( true ) );
                j++;
            }
            i++;
        }
    }

    @Test
    //    @Ignore
    public void cycleFromGraph1MissingInFilteredGraph2WhenOneRelationshipInCycleFilteredOut()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef a = new ProjectVersionRef( "project", "A", "1.0" );
        final ProjectVersionRef b = new ProjectVersionRef( "project", "B", "1.0" );
        final ProjectVersionRef c = new ProjectVersionRef( "project", "C", "1.0" );

        final ProjectVersionRef d = new ProjectVersionRef( "project", "D", "1.0" );
        final ProjectVersionRef e = new ProjectVersionRef( "project", "E", "1.0" );

        /* @formatter:off */
        // a --> b --> c --> a
        // d --> e --> c --> a --> b --> c
        getManager().storeRelationships( new DependencyRelationship( source, a, new ArtifactRef( b, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, c,  new ArtifactRef( a,  null, null, false ), null, 0, false ),
                                         new PluginRelationship( source, b,  c, 0, false ),
                                         new DependencyRelationship( source, d, new ArtifactRef( e, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, e, new ArtifactRef( c,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final GraphWorkspace session = simpleSession();
        final EProjectGraph graph1 = getManager().getGraph( session, a );
        final EProjectGraph graph2 = getManager().getGraph( session, d )
                                                 .filteredInstance( new DependencyFilter() );

        final Set<EProjectCycle> cycles1 = graph1.getCycles();
        System.out.println( "Graph 1 Cycles:\n\n" + join( cycles1, "\n" ) );

        final Set<EProjectCycle> cycles2 = graph2.getCycles();
        System.out.println( "Graph 2 Cycles:\n\n" + join( cycles2, "\n" ) );

        assertThat( cycles1.size(), equalTo( 1 ) );
        assertThat( cycles2.size(), equalTo( 0 ) );

        final Set<Set<EProjectCycle>> cycleSets = new HashSet<Set<EProjectCycle>>();
        cycleSets.add( cycles1 );
        //        cycleSets.add( cycles2 );

        int i = 0;
        for ( final Set<EProjectCycle> cycles : cycleSets )
        {
            int j = 0;
            for ( final EProjectCycle cycle : cycles )
            {
                final Set<ProjectVersionRef> refs = cycle.getAllParticipatingProjects();
                assertThat( i + ", " + j + " missing A", refs.contains( a ), equalTo( true ) );
                assertThat( i + ", " + j + " missing B", refs.contains( b ), equalTo( true ) );
                assertThat( i + ", " + j + " missing C", refs.contains( c ), equalTo( true ) );
                j++;
            }
            i++;
        }
    }

}
