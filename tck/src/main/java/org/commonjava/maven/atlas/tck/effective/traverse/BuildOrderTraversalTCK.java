/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.maven.atlas.tck.effective.traverse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.filter.DependencyFilter;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.traverse.BuildOrderTraversal;
import org.apache.maven.graph.effective.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.tck.effective.AbstractEGraphTCK;
import org.junit.Test;

public abstract class BuildOrderTraversalTCK
    extends AbstractEGraphTCK
{

    @Test
    public void simpleDependencyBuildOrder()
        throws Exception
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c, newDriverInstance() ).withDependencies( new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               b,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               a,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ) )
                                                               .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 2 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_includeDepParent()
        throws Exception

    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "group.id", "b-parent", "1001" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c, newDriverInstance() ).withDependencies( new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               b,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               a,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ) )
                                                               .withExactRelationships( new ParentRelationship( b, p ) )
                                                               .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertThat( buildOrder.size(), equalTo( 4 ) );

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( p.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_IgnorePluginPath()
        throws Exception
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c, newDriverInstance() ).withDependencies( new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               b,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               a,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              pb,
                                                                                                              new ArtifactRef(
                                                                                                                               pa,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ) )
                                                               .withPlugins( new PluginRelationship( c, pb, 0, false ) )
                                                               .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_runtimeDepsOnly()
        throws Exception
    {
        final ProjectVersionRef e = new ProjectVersionRef( "group.id", "e", "5" );
        final ProjectVersionRef d = new ProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c, newDriverInstance() ).withDependencies( new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               b,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               a,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              DependencyScope.runtime,
                                                                                                              0, false ),
                                                                                  new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               d,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              DependencyScope.test,
                                                                                                              1, false ),
                                                                                  new DependencyRelationship(
                                                                                                              d,
                                                                                                              new ArtifactRef(
                                                                                                                               e,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              DependencyScope.runtime,
                                                                                                              0, false ),
                                                                                  new DependencyRelationship(
                                                                                                              pb,
                                                                                                              new ArtifactRef(
                                                                                                                               pa,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ) )
                                                               .withPlugins( new PluginRelationship( c, pb, 0, false ) )
                                                               .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 6 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertThat( buildOrder.size(), equalTo( 3 ) );

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_ignoreExcluded()
        throws Exception
    {
        final ProjectVersionRef d = new ProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c, newDriverInstance() ).withDependencies( new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               b,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null,
                                                                                                              0,
                                                                                                              false,
                                                                                                              d.asProjectRef() ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               a,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              DependencyScope.runtime,
                                                                                                              0, false ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               d,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              DependencyScope.runtime,
                                                                                                              1, false ) )
                                                               .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertThat( buildOrder.size(), equalTo( 3 ) );

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleEverythingBuildOrder()
        throws Exception
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.dep.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c, newDriverInstance() ).withDependencies( new DependencyRelationship(
                                                                                                              c,
                                                                                                              new ArtifactRef(
                                                                                                                               b,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              b,
                                                                                                              new ArtifactRef(
                                                                                                                               a,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ),
                                                                                  new DependencyRelationship(
                                                                                                              pb,
                                                                                                              new ArtifactRef(
                                                                                                                               pa,
                                                                                                                               null,
                                                                                                                               null,
                                                                                                                               false ),
                                                                                                              null, 0,
                                                                                                              false ) )
                                                               .withPlugins( new PluginRelationship( c, pb, 0, false ) )
                                                               .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal();
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( pa.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( pb.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

}
