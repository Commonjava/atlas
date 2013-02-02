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

import static org.apache.maven.graph.effective.util.EGraphUtils.dependency;
import static org.apache.maven.graph.effective.util.EGraphUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.EProjectRelationships;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.traverse.TransitiveDependencyTraversal;
import org.commonjava.maven.atlas.tck.effective.AbstractSPI_TCK;
import org.junit.Test;

public abstract class TransitiveDependencyTraversalTCK
    extends AbstractSPI_TCK
{

    @Test
    public void collectDependencyOfDependency()
        throws Exception
    {
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( root, newDriverInstance() );

        final DependencyRelationship depL1 = dependency( root, "other.group", "dep-L1", "1.0.1", 0 );
        final DependencyRelationship depL2 = dependency( depL1.getTarget()
                                                              .asProjectVersionRef(), "foo", "dep-L2", "1.1.1", 0 );

        pgBuilder.withDependencies( depL1 );

        final EProjectRelationships depL1Rels =
            new EProjectRelationships.Builder( depL1.getTarget()
                                                    .asProjectVersionRef() ).withDependencies( depL2 )
                                                                            .build();
        pgBuilder.withDirectProjectRelationships( depL1Rels );

        final EProjectGraph graph = pgBuilder.build();

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
    }

    @Test
    public void preferDirectDependency()
        throws Exception
    {
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( root, newDriverInstance() );

        final DependencyRelationship depL1A = dependency( root, "other.group", "dep-L1", "1.0.1", 0 );
        final DependencyRelationship depL1B = dependency( root, "foo", "dep-L2", "1.1.1", 1 );
        final DependencyRelationship depL2 = dependency( depL1A.getTarget()
                                                               .asProjectVersionRef(), "foo", "dep-L2", "1.0", 0 );

        pgBuilder.withDependencies( depL1A, depL1B );

        final EProjectRelationships depL1Rels =
            new EProjectRelationships.Builder( depL1A.getTarget()
                                                     .asProjectVersionRef() ).withDependencies( depL2 )
                                                                             .build();
        pgBuilder.withDirectProjectRelationships( depL1Rels );

        final EProjectGraph graph = pgBuilder.build();

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
        assertThat( ref.getVersionSpec()
                       .renderStandard(), equalTo( "1.1.1" ) );
    }

    @Test
    public void preferDirectDependencyInParent()
        throws Exception
    {
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( root, newDriverInstance() );

        final DependencyRelationship depL1A = dependency( root, "other.group", "dep-L1", "1.0.1", 0 );
        final DependencyRelationship depL2 = dependency( depL1A.getTarget()
                                                               .asProjectVersionRef(), "foo", "dep-L2", "1.0", 0 );

        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );

        pgBuilder.withDependencies( depL1A );
        pgBuilder.withParent( parent );

        final EProjectRelationships depL1Rels =
            new EProjectRelationships.Builder( depL1A.getTarget()
                                                     .asProjectVersionRef() ).withDependencies( depL2 )
                                                                             .build();

        final DependencyRelationship depL1B = dependency( parent, "foo", "dep-L2", "1.1.1", 1 );

        final EProjectRelationships parentRels = new EProjectRelationships.Builder( parent ).withDependencies( depL1B )
                                                                                            .build();

        pgBuilder.withDirectProjectRelationships( parentRels, depL1Rels );

        final EProjectGraph graph = pgBuilder.build();

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
        assertThat( ref.getVersionSpec()
                       .renderStandard(), equalTo( "1.1.1" ) );
    }

    @Test
    public void preferLocalDirectDepOverDirectDepInParent()
        throws Exception
    {
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( root, newDriverInstance() );

        final DependencyRelationship depL1A = dependency( root, "other.group", "dep-L1", "1.1.1", 0 );

        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );

        pgBuilder.withDependencies( depL1A );
        pgBuilder.withParent( parent );

        final DependencyRelationship depL1B = dependency( parent, "other.group", "dep-L1", "1.0", 1 );

        final EProjectRelationships parentRels = new EProjectRelationships.Builder( parent ).withDependencies( depL1B )
                                                                                            .build();

        pgBuilder.withDirectProjectRelationships( parentRels );

        final EProjectGraph graph = pgBuilder.build();

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 1 ) );

        int idx = 0;

        final ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        assertThat( ref.getVersionSpec()
                       .renderStandard(), equalTo( "1.1.1" ) );
    }

}
