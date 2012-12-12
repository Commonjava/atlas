package org.apache.maven.graph.effective.traverse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.filter.DependencyFilter;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.junit.Test;

public class BuildOrderTraversalTest
{

    @Test
    public void simpleDependencyBuildOrder()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c ).withDependencies( new DependencyRelationship( c, new ArtifactRef( b, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ),
                                                             new DependencyRelationship( b, new ArtifactRef( a, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ) )
                                          .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 2 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final List<ProjectRef> buildOrder = bo.getBuildOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_IgnorePluginPath()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c ).withDependencies( new DependencyRelationship( c, new ArtifactRef( b, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ),
                                                             new DependencyRelationship( b, new ArtifactRef( a, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ),
                                                             new DependencyRelationship( pb, new ArtifactRef( pa, null,
                                                                                                              null,
                                                                                                              false ),
                                                                                         null, 0, false ) )
                                          .withPlugins( new PluginRelationship( c, pb, 0, false ) )
                                          .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final List<ProjectRef> buildOrder = bo.getBuildOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_runtimeDepsOnly()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef e = new ProjectVersionRef( "group.id", "e", "5" );
        final ProjectVersionRef d = new ProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c ).withDependencies( new DependencyRelationship( c, new ArtifactRef( b, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ),
                                                             new DependencyRelationship( b, new ArtifactRef( a, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         DependencyScope.runtime, 0,
                                                                                         false ),
                                                             new DependencyRelationship( c, new ArtifactRef( d, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         DependencyScope.test, 1, false ),
                                                             new DependencyRelationship( d, new ArtifactRef( e, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         DependencyScope.runtime, 0,
                                                                                         false ),
                                                             new DependencyRelationship( pb, new ArtifactRef( pa, null,
                                                                                                              null,
                                                                                                              false ),
                                                                                         null, 0, false ) )
                                          .withPlugins( new PluginRelationship( c, pb, 0, false ) )
                                          .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 6 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final List<ProjectRef> buildOrder = bo.getBuildOrder();

        assertThat( buildOrder.size(), equalTo( 3 ) );

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleDependencyBuildOrder_ignoreExcluded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef d = new ProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c ).withDependencies( new DependencyRelationship( c, new ArtifactRef( b, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false,
                                                                                         d.asProjectRef() ),
                                                             new DependencyRelationship( b, new ArtifactRef( a, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         DependencyScope.runtime, 0,
                                                                                         false ),
                                                             new DependencyRelationship( b, new ArtifactRef( d, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         DependencyScope.runtime, 1,
                                                                                         false ) )
                                          .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final List<ProjectRef> buildOrder = bo.getBuildOrder();

        assertThat( buildOrder.size(), equalTo( 3 ) );

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

    @Test
    public void simpleEverythingBuildOrder()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.dep.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final EProjectGraph graph =
            new EProjectGraph.Builder( c ).withDependencies( new DependencyRelationship( c, new ArtifactRef( b, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ),
                                                             new DependencyRelationship( b, new ArtifactRef( a, null,
                                                                                                             null,
                                                                                                             false ),
                                                                                         null, 0, false ),
                                                             new DependencyRelationship( pb, new ArtifactRef( pa, null,
                                                                                                              null,
                                                                                                              false ),
                                                                                         null, 0, false ) )
                                          .withPlugins( new PluginRelationship( c, pb, 0, false ) )
                                          .build();

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal();
        graph.traverse( bo );

        final List<ProjectRef> buildOrder = bo.getBuildOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( pa.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( pb.asProjectRef() ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c.asProjectRef() ) );
    }

}
