package org.apache.maven.graph.effective.traverse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.EProjectGraph;
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

        final BuildOrderTraversal bo = new BuildOrderTraversal( RelationshipType.DEPENDENCY );
        graph.traverse( bo );

        final List<ProjectVersionRef> buildOrder = bo.getBuildOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c ) );
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

        final BuildOrderTraversal bo = new BuildOrderTraversal( RelationshipType.DEPENDENCY );
        graph.traverse( bo );

        final List<ProjectVersionRef> buildOrder = bo.getBuildOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c ) );
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

        final List<ProjectVersionRef> buildOrder = bo.getBuildOrder();

        int idx = 0;
        assertThat( buildOrder.get( idx++ ), equalTo( a ) );
        assertThat( buildOrder.get( idx++ ), equalTo( b ) );
        assertThat( buildOrder.get( idx++ ), equalTo( pa ) );
        assertThat( buildOrder.get( idx++ ), equalTo( pb ) );
        assertThat( buildOrder.get( idx++ ), equalTo( c ) );
    }

}
