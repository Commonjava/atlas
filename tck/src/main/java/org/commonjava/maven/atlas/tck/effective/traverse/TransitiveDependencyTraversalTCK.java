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
package org.commonjava.maven.atlas.tck.effective.traverse;

import static org.apache.maven.graph.common.util.IdentityUtils.projectVersion;
import static org.apache.maven.graph.effective.util.RelationshipUtils.dependency;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.EProjectRelationships;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
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
        final URI source = sourceURI();
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( source, root, newDriverInstance() );

        final DependencyRelationship depL1 = dependency( source, root, "other.group", "dep-L1", "1.0.1", 0 );
        final DependencyRelationship depL2 =
            dependency( source, depL1.getTarget()
                                     .asProjectVersionRef(), "foo", "dep-L2", "1.1.1", 0 );

        pgBuilder.withDependencies( depL1 );

        final EProjectRelationships depL1Rels =
            new EProjectRelationships.Builder( source, depL1.getTarget()
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
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( source, root, newDriverInstance() );

        final DependencyRelationship depL1A = dependency( source, root, "other.group", "dep-L1", "1.0.1", 0 );
        final DependencyRelationship depL1B = dependency( source, root, "foo", "dep-L2", "1.1.1", 1 );
        final DependencyRelationship depL2 =
            dependency( source, depL1A.getTarget()
                                      .asProjectVersionRef(), "foo", "dep-L2", "1.0", 0 );

        pgBuilder.withDependencies( depL1A, depL1B );

        final EProjectRelationships depL1Rels =
            new EProjectRelationships.Builder( source, depL1A.getTarget()
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
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( source, root, newDriverInstance() );

        final DependencyRelationship depL1A = dependency( source, root, "other.group", "dep-L1", "1.0.1", 0 );
        final DependencyRelationship depL2 =
            dependency( source, depL1A.getTarget()
                                      .asProjectVersionRef(), "foo", "dep-L2", "1.0", 0 );

        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );

        pgBuilder.withDependencies( depL1A );
        pgBuilder.withParent( new ParentRelationship( source, root, parent ) );

        final EProjectRelationships depL1Rels =
            new EProjectRelationships.Builder( source, depL1A.getTarget()
                                                             .asProjectVersionRef() ).withDependencies( depL2 )
                                                                                     .build();

        final DependencyRelationship depL1B = dependency( source, parent, "foo", "dep-L2", "1.1.1", 1 );

        final EProjectRelationships parentRels =
            new EProjectRelationships.Builder( source, parent ).withDependencies( depL1B )
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
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( source, root, newDriverInstance() );

        final DependencyRelationship depL1A = dependency( source, root, "other.group", "dep-L1", "1.1.1", 0 );

        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );

        pgBuilder.withDependencies( depL1A );
        pgBuilder.withParent( new ParentRelationship( source, root, parent ) );

        final DependencyRelationship depL1B = dependency( source, parent, "other.group", "dep-L1", "1.0", 1 );

        final EProjectRelationships parentRels =
            new EProjectRelationships.Builder( source, parent ).withDependencies( depL1B )
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
