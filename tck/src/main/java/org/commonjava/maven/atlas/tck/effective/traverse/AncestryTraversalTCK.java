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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;

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
        final ProjectVersionRef myRef = new ProjectVersionRef( "my.group", "my-artifact", "1.0" );
        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );

        /* @formatter:off */
        final EProjectGraph graph = new EProjectGraph.Builder( myRef, newDriverInstance() )
            .withParent( parentRef )
            .withDirectProjectRelationships( new EProjectRelationships.Builder( parentRef ).withParent( grandRef ).build() )
            .build();
        /* @formatter:on */

        final Set<ProjectVersionRef> projects = graph.getAllProjects();
        assertThat( projects.size(), equalTo( 3 ) );
        assertThat( projects.contains( myRef ), equalTo( true ) );
        assertThat( projects.contains( parentRef ), equalTo( true ) );
        assertThat( projects.contains( grandRef ), equalTo( true ) );

        final AncestryTraversal ancestry = new AncestryTraversal();
        graph.traverse( ancestry );

        final List<ProjectVersionRef> ancestorRefs = ancestry.getAncestry();

        logger.info( "Ancestry: %s", ancestorRefs );

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
        final ProjectVersionRef myRef = new ProjectVersionRef( "my.group", "my-artifact", "1.0" );
        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );

        /* @formatter:off */
        final EProjectGraph graph = new EProjectGraph.Builder( myRef, newDriverInstance() )
            .withParent( parentRef )
            .withDirectProjectRelationships( new EProjectRelationships.Builder( parentRef ).withParent( grandRef ).build() )
            .build();
        /* @formatter:on */

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
        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );

        /* @formatter:off */
        final EProjectGraph graph = new EProjectGraph.Builder( myRef, newDriverInstance() )
            .withParent( parentRef )
            .withDependencies(
                  new DependencyRelationship( myRef, new ArtifactRef( new ProjectVersionRef( "some.group", "foo", "1.0"   ), null, null, false ), null, 0, false ),
                  new DependencyRelationship( myRef, new ArtifactRef( new ProjectVersionRef( "some.group", "bar", "1.2.1" ), null, null, false ), null, 1, false )
            )
            .withPlugins(
                 new PluginRelationship( myRef, new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.5.1" ), 0, false ),
                 new PluginRelationship( myRef, new ProjectVersionRef( "org.apache.maven.plugins","maven-jar-plugin", "2.2" ), 1, false )
            )
            .withExtensions(
                new ExtensionRelationship( myRef, new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.5.1" ), 0 )
            )
            .withDirectProjectRelationships( 
                 new EProjectRelationships.Builder( parentRef )
                     .withParent( grandRef )
                     .withDependencies( 
                        new DependencyRelationship( parentRef, new ArtifactRef( new ProjectVersionRef( "other.group", "utils", "3-1" ), null, null, false ), null, 0, false )
                     )
                     .build()
            )
            .build();
        /* @formatter:on */

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
