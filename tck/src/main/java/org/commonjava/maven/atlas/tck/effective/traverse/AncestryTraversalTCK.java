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

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.EProjectRelationships;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.tck.effective.AbstractSPI_TCK;
import org.junit.Test;

public abstract class AncestryTraversalTCK
    extends AbstractSPI_TCK
{

    @Test
    public void traverseTwoAncestors()
        throws Exception
    {
        final EProjectGraph.Builder pgBuilder =
            new EProjectGraph.Builder( new ProjectVersionRef( "my.group", "my-artifact", "1.0" ), newDriverInstance() );
        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        pgBuilder.withParent( parentRef );

        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );
        final EProjectRelationships parentRels = new EProjectRelationships.Builder( parentRef ).withParent( grandRef )
                                                                                               .build();

        pgBuilder.withDirectProjectRelationships( parentRels );

        final EProjectGraph graph = pgBuilder.build();

        final AncestryTraversal ancestry = new AncestryTraversal();
        graph.traverse( ancestry );

        final List<ProjectVersionRef> ancestorRefs = ancestry.getAncestry();

        assertThat( ancestorRefs.size(), equalTo( 3 ) );

        int idx = 0;
        ProjectVersionRef ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-artifact" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-dad" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "other.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "grandpa" ) );

    }

    @Test
    public void traverseTwoAncestorsWithEmptyGrandParentRels()
        throws Exception
    {
        final EProjectGraph.Builder pgBuilder =
            new EProjectGraph.Builder( new ProjectVersionRef( "my.group", "my-artifact", "1.0" ), newDriverInstance() );
        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        pgBuilder.withParent( parentRef );

        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );
        final EProjectRelationships parentRels = new EProjectRelationships.Builder( parentRef ).withParent( grandRef )
                                                                                               .build();

        pgBuilder.withDirectProjectRelationships( parentRels );

        final EProjectGraph graph = pgBuilder.build();

        final AncestryTraversal ancestry = new AncestryTraversal();
        graph.traverse( ancestry );

        final List<ProjectVersionRef> ancestorRefs = ancestry.getAncestry();

        assertThat( ancestorRefs.size(), equalTo( 3 ) );

        int idx = 0;
        ProjectVersionRef ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-artifact" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-dad" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "other.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "grandpa" ) );

    }

    @Test
    public void traverseTwoAncestors_IgnoreNonParentRelationships()
        throws Exception
    {
        final ProjectVersionRef myRef = new ProjectVersionRef( "my.group", "my-artifact", "1.0" );

        final EProjectGraph.Builder pgBuilder = new EProjectGraph.Builder( myRef, newDriverInstance() );

        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        pgBuilder.withParent( parentRef );

        pgBuilder.withDependencies( new DependencyRelationship( myRef,
                                                                new ArtifactRef( new ProjectVersionRef( "some.group",
                                                                                                        "foo", "1.0" ),
                                                                                 null, null, false ), null, 0, false ),
                                    new DependencyRelationship(
                                                                myRef,
                                                                new ArtifactRef(
                                                                                 new ProjectVersionRef( "some.group",
                                                                                                        "bar", "1.2.1" ),
                                                                                 null, null, false ), null, 1, false ) );

        pgBuilder.withPlugins( new PluginRelationship( myRef,
                                                       new ProjectVersionRef( "org.apache.maven.plugins",
                                                                              "maven-compiler-plugin", "2.5.1" ), 0,
                                                       false ),
                               new PluginRelationship( myRef, new ProjectVersionRef( "org.apache.maven.plugins",
                                                                                     "maven-jar-plugin", "2.2" ), 1,
                                                       false ) );

        pgBuilder.withExtensions( new ExtensionRelationship( myRef, new ProjectVersionRef( "org.apache.maven.plugins",
                                                                                           "maven-compiler-plugin",
                                                                                           "2.5.1" ), 0 ) );

        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );
        final EProjectRelationships parentRels =
            new EProjectRelationships.Builder( parentRef ).withParent( grandRef )
                                                          .withDependencies( new DependencyRelationship(
                                                                                                         parentRef,
                                                                                                         new ArtifactRef(
                                                                                                                          new ProjectVersionRef(
                                                                                                                                                 "other.group",
                                                                                                                                                 "utils",
                                                                                                                                                 "3-1" ),
                                                                                                                          null,
                                                                                                                          null,
                                                                                                                          false ),
                                                                                                         null, 0, false ) )
                                                          .build();

        pgBuilder.withDirectProjectRelationships( parentRels );

        final EProjectGraph graph = pgBuilder.build();

        final AncestryTraversal ancestry = new AncestryTraversal();
        graph.traverse( ancestry );

        final List<ProjectVersionRef> ancestorRefs = ancestry.getAncestry();

        assertThat( ancestorRefs.size(), equalTo( 3 ) );

        int idx = 0;
        ProjectVersionRef ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-artifact" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-dad" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "other.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "grandpa" ) );

    }

}
