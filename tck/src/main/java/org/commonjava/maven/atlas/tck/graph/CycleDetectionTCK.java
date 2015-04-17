/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.tck.graph;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
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

        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new DependencyRelationship( source, project, new ArtifactRef( dep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep,  new ArtifactRef( dep2,  null, null, false ), null, 0, false ) );
        
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

        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new DependencyRelationship( source, project, dep.asJarArtifact(), null, 0, false ),
                                  new DependencyRelationship( source, dep,  dep2.asJarArtifact(), null, 0, false ),
                                  new DependencyRelationship( source, dep2,  project.asJarArtifact(), null, 0, false ) );
        /* @formatter:on */

        //        final EProjectGraph graph = getManager().getGraph( session, project );

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

        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        final Set<ProjectRelationship<?>> rejected = graph.storeRelationships( 
                                         new DependencyRelationship( source, project, new ArtifactRef( dep, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep,  new ArtifactRef( dep2,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, dep2,  new ArtifactRef( dep,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        assertThat( rejected, notNullValue() );

        //        System.out.println( "Rejects: " + rejected );
        //
        //        assertThat( rejected.size(), equalTo( 2 ) );
        //        final ProjectRelationship<?> reject = rejected.iterator()
        //                                                      .next();
        //        assertThat( reject.getDeclaring(), equalTo( dep2 ) );
        //        assertThat( reject.getTarget()
        //                          .asProjectVersionRef(), equalTo( dep ) );

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

        final RelationshipGraph graph = simpleGraph( a );

        /* @formatter:off */
        // a --> b --> c --> a
        // d --> e --> c --> a --> b --> c
        graph.storeRelationships( new DependencyRelationship( source, a, new ArtifactRef( b, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, b,  new ArtifactRef( c,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, c,  new ArtifactRef( a,  null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, d, new ArtifactRef( e, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, e,  new ArtifactRef( c,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final RelationshipGraph graph2 =
            graphFactory().open( new ViewParams.Builder( graph.getParams() ).withRoots( d )
                                                                            .build(), false );

        final Set<EProjectCycle> cycles1 = graph.getCycles();
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

        final RelationshipGraph graph = simpleGraph( a );

        /* @formatter:off */
        // a --> b --> c --> a
        // d --> e --> c --> a --> b --> c
        graph.storeRelationships( new DependencyRelationship( source, a, new ArtifactRef( b, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, c,  new ArtifactRef( a,  null, null, false ), null, 0, false ),
                                         new PluginRelationship( source, b,  c, 0, false ),
                                         new DependencyRelationship( source, d, new ArtifactRef( e, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, e, new ArtifactRef( c,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final RelationshipGraph graph2 =
            graphFactory().open( new ViewParams.Builder( graph.getParams() ).withFilter( new DependencyFilter() )
                                                                            .withRoots( d )
                                                                            .build(), false );

        final Set<EProjectCycle> cycles1 = graph.getCycles();
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
