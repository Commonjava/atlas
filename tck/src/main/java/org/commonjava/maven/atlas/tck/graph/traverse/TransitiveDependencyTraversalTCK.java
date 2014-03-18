/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.tck.graph.traverse;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.dependency;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.TransitiveDependencyTraversal;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
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
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2 = projectVersion( "foo", "dep-L2", "1.1.1" );

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                simpleWorkspace(), 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, root ) )
                    .withDependencies( dependency( source, root, d1, 0 ) )
                    .build() 
        );
        
        graph.addAll( Arrays.asList(
                dependency( source, d1, d2, 0 )
        ));
        /* @formatter:on */

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
    public void collectDependencyOfDependencyWithExternalDepManagement()
        throws Exception
    {
        final URI source = sourceURI();
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2 = projectVersion( "foo", "dep-L2", "1.1.1" );
        final ProjectVersionRef d3 = projectVersion( "foo", "dep-L2", "1.1.2" );

        final GraphView view = new GraphView( simpleWorkspace(), AnyFilter.INSTANCE, new ManagedDependencyMutator(), root );

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                view, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, root ) )
                    .withDependencies( dependency( source, root, d1, 0 ) )
                    .build() 
        );
        
        graph.addAll( Arrays.asList(
                dependency( source, d1, d2, 0 )
        ));
        /* @formatter:on */

        graph.getView()
             .selectVersion( d2.asProjectRef(), d3 );

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
        assertThat( ref.getVersionString(), equalTo( d3.getVersionString() ) );
    }

    @Test
    public void preferDirectDependency()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2a = projectVersion( "foo", "dep-L2", "1.1.1" );
        final ProjectVersionRef d2b = projectVersion( "foo", "dep-L2", "1.0" );

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                simpleWorkspace(), 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, root ) )
                    .withDependencies( 
                        dependency( source, root, d1, 0 ),
                        dependency( source, root, d2a, 1 )
                    )
                    .build() 
        );
        
        graph.addAll( Arrays.asList(
                dependency( source, d1, d2b, 0 )
        ));
        /* @formatter:on */

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
        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2a = projectVersion( "foo", "dep-L2", "1.1.1" );
        final ProjectVersionRef d2b = projectVersion( "foo", "dep-L2", "1.0" );

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                simpleWorkspace(), 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, root ) )
                    .withDependencies( 
                        dependency( source, root, d1, 0 )
                    )
                    .withParent( new ParentRelationship( source, root, parent ) )
                    .build() 
        );
        
        graph.addAll( Arrays.asList(
                dependency( source, parent, d2a, 0 ),
                dependency( source, d1, d2b, 0 )
        ));
        /* @formatter:on */

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
        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );
        final ProjectVersionRef d1a = projectVersion( "other.group", "dep-L1", "1.1.1" );
        final ProjectVersionRef d1b = projectVersion( "other.group", "dep-L1", "1.0" );

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                simpleWorkspace(), 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, root ) )
                    .withDependencies( 
                        dependency( source, root, d1a, 0 )
                    )
                    .withParent( new ParentRelationship( source, root, parent ) )
                    .build() 
        );
        
        graph.addAll( Arrays.asList(
                dependency( source, parent, d1b, 0 )
        ));
        /* @formatter:on */

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
