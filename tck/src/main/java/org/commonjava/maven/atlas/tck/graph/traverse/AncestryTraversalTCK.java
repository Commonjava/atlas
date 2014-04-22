/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.tck.graph.traverse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
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
        final EProjectGraph graph = getManager().createGraph( simpleWorkspace(), new EProjectDirectRelationships.Builder( new EProjectKey( source, myRef ) ).build() );
        graph.addAll( Arrays.asList( new ParentRelationship( source, myRef, parentRef ), new ParentRelationship( source, parentRef, grandRef ), new ParentRelationship( source, grandRef ) ) );
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
        final EProjectGraph graph = getManager().createGraph( simpleWorkspace(), new EProjectDirectRelationships.Builder( new EProjectKey( source, myRef ) ).build() );
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
        final EProjectGraph graph = getManager().createGraph( simpleWorkspace(), new EProjectDirectRelationships.Builder( new EProjectKey( source, myRef ) )
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
