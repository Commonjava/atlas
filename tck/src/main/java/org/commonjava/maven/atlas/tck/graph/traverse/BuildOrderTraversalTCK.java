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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.commonjava.util.logging.Logger;
import org.junit.Test;

public abstract class BuildOrderTraversalTCK
    extends AbstractSPI_TCK
{

    @Test
    public void simpleDependencyBuildOrder()
        throws Exception
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();

        /* @formatter:off */
        getManager().storeRelationships( ws, new DependencyRelationship( source, c, new ArtifactRef( b, null, null, false ), null, 0, false ),
                                       new DependencyRelationship( source, b, new ArtifactRef( a, null, null, false ), null, 0, false ) );
        
        final EProjectGraph graph = getManager().getGraph( ws, c );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 2 ) );

        logger.info( "Starting build-order traversal" );
        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_includeDepParent()
        throws Exception

    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef p = new ProjectVersionRef( "group.id", "b-parent", "1001" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );
        relativeOrder.put( b, p );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                ws, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                    .withDependencies( new DependencyRelationship( source, c, new ArtifactRef( b, null, null, false ), null, 0, false ) )
                    .build()
        );
        
        graph.addAll( Arrays.asList( 
                new ParentRelationship( source, b, p ),
                new DependencyRelationship( source, b, new ArtifactRef( a, null, null, false ), null, 0, false )
        ) );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        new Logger( getClass() ).info( "Build order: %s", buildOrder );

        assertThat( buildOrder.size(), equalTo( 4 ) );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_IgnorePluginPath()
        throws Exception
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                ws, 
                new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                    .withDependencies( new DependencyRelationship( source, c, new ArtifactRef( b, null, null, false ), null, 0, false ) )
                    .withPlugins( new PluginRelationship( source, c, pb, 0, false ) )
                    .build()
        );
        
        graph.addAll( Arrays.asList( 
                new DependencyRelationship( source, pb, new ArtifactRef( pa, null, null, false ), null, 0, false ),
                new DependencyRelationship( source, b, new ArtifactRef( a, null, null, false ), null, 0, false )
        ) );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
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

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                  ws, 
                  new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                      .withDependencies( new DependencyRelationship( source, c, new ArtifactRef( b, null, null, false ), null, 0, false ),
                                         new DependencyRelationship( source, c, new ArtifactRef( d, null, null, false ), DependencyScope.test, 1, false )
                      )
                      .withPlugins( new PluginRelationship( source, c, pb, 0, false ) )
                      .build()
        );
        
        graph.addAll( Arrays.asList(
                new DependencyRelationship( source, b, new ArtifactRef( a, null, null, false ), DependencyScope.runtime, 0, false ),
                new DependencyRelationship( source, d, new ArtifactRef( e, null, null, false ), DependencyScope.runtime, 0, false ),
                new DependencyRelationship( source, pb, new ArtifactRef( pa, null, null, false ), null, 0, false )
        ));
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 6 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertThat( buildOrder.size(), equalTo( 3 ) );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_ignoreExcluded()
        throws Exception
    {
        final ProjectVersionRef d = new ProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                  ws, 
                  new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                      .withDependencies( new DependencyRelationship( source, c, new ArtifactRef( b, null, null, false ), null, 0, false, d )
                      )
                      .build()
        );
        
        graph.addAll( Arrays.asList(
                new DependencyRelationship( source, b, new ArtifactRef( a, null, null, false ), DependencyScope.runtime, 0, false ),
                new DependencyRelationship( source, b, new ArtifactRef( d, null, null, false ), DependencyScope.runtime, 1, false )
        ));
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        logger.info( "Build order: %s", buildOrder );
        assertThat( buildOrder.size(), equalTo( 3 ) );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleEverythingBuildOrder()
        throws Exception
    {
        final ProjectVersionRef c = new ProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new ProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new ProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new ProjectVersionRef( "plugin.dep.id", "p-a", "1" );
        final ProjectVersionRef pb = new ProjectVersionRef( "plugin.id", "p-b", "2" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder = new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );
        relativeOrder.put( c, pb );
        relativeOrder.put( pb, pa );

        final URI source = sourceURI();
        final GraphWorkspace ws = simpleWorkspace();

        /* @formatter:off */
        final EProjectGraph graph = getManager().createGraph( 
                  ws, 
                  new EProjectDirectRelationships.Builder( new EProjectKey( source, c ) )
                      .withDependencies( new DependencyRelationship( source, c, new ArtifactRef( b, null, null, false ), null, 0, false )
                      )
                      .withPlugins( new PluginRelationship( source, c, pb, 0, false ) )
                      .build()
        );
        
        graph.addAll( Arrays.asList(
                new DependencyRelationship( source, b, new ArtifactRef( a, null, null, false ), DependencyScope.runtime, 0, false ),
                new DependencyRelationship( source, pb, new ArtifactRef( pa, null, null, false ), null, 0, false )
        ));
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal();
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        System.out.printf( "Build order: %s\n", buildOrder );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    private void assertRelativeOrder( final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder, final List<ProjectRef> buildOrder )
    {
        for ( final Map.Entry<ProjectVersionRef, ProjectVersionRef> entry : relativeOrder.entrySet() )
        {
            final ProjectRef k = entry.getKey()
                                      .asProjectRef();
            final ProjectRef v = entry.getValue()
                                      .asProjectRef();

            final int kidx = buildOrder.indexOf( k );
            final int vidx = buildOrder.indexOf( v );

            if ( kidx < 0 )
            {
                fail( "Cannot find: " + k + " in build order: " + buildOrder );
            }

            if ( vidx < 0 )
            {
                fail( "Cannot find: " + v + " in build order: " + buildOrder );
            }

            if ( vidx >= kidx )
            {
                fail( "prerequisite project: " + v + " of: " + k + " appears AFTER it in the build order: " + buildOrder );
            }
        }
    }

}
