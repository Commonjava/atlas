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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.common.ref.ArtifactRef;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.EProjectDirectRelationships;
import org.commonjava.maven.atlas.effective.EProjectGraph;
import org.commonjava.maven.atlas.effective.ref.EProjectKey;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.effective.rel.ParentRelationship;
import org.commonjava.maven.atlas.effective.rel.PluginRelationship;
import org.commonjava.maven.atlas.effective.traverse.AncestryTraversal;
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

        final URI source = sourceURI();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( simpleSession(), new EProjectDirectRelationships.Builder( new EProjectKey( source, myRef ) ).build() );
        graph.addAll( Arrays.asList( new ParentRelationship( source, myRef, parentRef ), new ParentRelationship( source, parentRef, grandRef ) ) );
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

        final URI source = sourceURI();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( simpleSession(), new EProjectDirectRelationships.Builder( new EProjectKey( source, myRef ) ).build() );
        graph.addAll( Arrays.asList( new ParentRelationship( source, myRef, parentRef ), new ParentRelationship( source, parentRef, grandRef ) ) );
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

    @SuppressWarnings( "unchecked" )
    @Test
    public void traverseTwoAncestors_IgnoreNonParentRelationships()
        throws Exception
    {
        final ProjectVersionRef myRef = new ProjectVersionRef( "my.group", "my-artifact", "1.0" );
        final ProjectVersionRef parentRef = new ProjectVersionRef( "my.group", "my-dad", "1" );
        final ProjectVersionRef grandRef = new ProjectVersionRef( "other.group", "grandpa", "20120821" );

        final URI source = sourceURI();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( simpleSession(), new EProjectDirectRelationships.Builder( new EProjectKey( source, myRef ) )
            .withParent( new ParentRelationship( source, myRef, parentRef ) )
            .withDependencies(
                  new DependencyRelationship( source, myRef, new ArtifactRef( new ProjectVersionRef( "some.group", "foo", "1.0"   ), null, null, false ), null, 0, false ),
                  new DependencyRelationship( source, myRef, new ArtifactRef( new ProjectVersionRef( "some.group", "bar", "1.2.1" ), null, null, false ), null, 1, false )
            )
            .withPlugins(
                 new PluginRelationship( source, myRef, new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.5.1" ), 0, false ),
                 new PluginRelationship( source, myRef, new ProjectVersionRef( "org.apache.maven.plugins","maven-jar-plugin", "2.2" ), 1, false )
            )
            .withExtensions(
                new ExtensionRelationship( source, myRef, new ProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.5.1" ), 0 )
            ).build()
        );
        
        graph.addAll( Arrays.asList( 
                new ParentRelationship( source, parentRef, grandRef ), 
                new DependencyRelationship( 
                        source, 
                        parentRef, 
                        new ArtifactRef( new ProjectVersionRef( "other.group", "utils", "3-1" ), null, null, false ), 
                        null, 
                        0, 
                        false 
                ) 
        ) );
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
