/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.graph.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.atlas.maven.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.atlas.maven.graph.model.EProjectDirectRelationships;
import org.commonjava.atlas.maven.graph.rel.DependencyRelationship;
import org.commonjava.atlas.maven.graph.rel.ExtensionRelationship;
import org.commonjava.atlas.maven.graph.rel.PluginDependencyRelationship;
import org.commonjava.atlas.maven.graph.rel.PluginRelationship;
import org.commonjava.atlas.maven.graph.rel.SimpleDependencyRelationship;
import org.commonjava.atlas.maven.graph.rel.SimpleExtensionRelationship;
import org.commonjava.atlas.maven.graph.rel.SimplePluginDependencyRelationship;
import org.commonjava.atlas.maven.graph.rel.SimplePluginRelationship;
import org.commonjava.atlas.maven.ident.DependencyScope;
import org.commonjava.atlas.maven.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 3/16/17.
 */
public class EProjectRelationshipsSerializerTest
{

    private ObjectMapper mapper;

    @Before
    public void before()
    {
        mapper = new ObjectMapper();
        mapper.registerModules( new ProjectVersionRefSerializerModule(), new ProjectRelationshipSerializerModule() );
    }

    @Test
    public void roundTrip_EProjectDirectRelationships()
            throws Exception
    {
        URI sourceUri = new URI( "test:source" );
        ProjectVersionRef p = new SimpleProjectVersionRef( "org.apache.maven", "maven-core", "3.0.3" );

        EProjectDirectRelationships.Builder prb = new EProjectDirectRelationships.Builder( sourceUri, p );

        ProjectVersionRef parent = new SimpleProjectVersionRef( "org.apache.maven", "maven", "3.0.3" );

        int idx = 0;
        int pidx = 0;
        int pdidx = 0;
        final DependencyRelationship papi =
                new SimpleDependencyRelationship( sourceUri, p, new SimpleArtifactRef( "org.apache.maven", "maven-plugin-api", "3.0.3",
                        null, null ), DependencyScope.compile,
                        idx++, false, false, false );
        final DependencyRelationship art =
                new SimpleDependencyRelationship( sourceUri, p, new SimpleArtifactRef( "org.apache.maven", "maven-artifact", "3.0.3",
                        null, null ), DependencyScope.compile,
                        idx++, false, false, false );
        final PluginRelationship jarp =
                new SimplePluginRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.plugins",
                        "maven-jar-plugin", "2.2" ), pidx++, false, false );
        final PluginRelationship comp =
                new SimplePluginRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.plugins",
                        "maven-compiler-plugin", "2.3.2" ), pidx++,
                        false, false );
        final PluginDependencyRelationship pdr =
                new SimplePluginDependencyRelationship( sourceUri, p,
                        new SimpleProjectRef( "org.apache.maven.plugins", "maven-compiler-plugin" ),
                        new SimpleArtifactRef( new SimpleProjectVersionRef("org.apache.test", "test", "1.1"), "pom", null ),
                        pdidx++,
                        false, false );
        final ExtensionRelationship wag =
                new SimpleExtensionRelationship( sourceUri, p, new SimpleProjectVersionRef( "org.apache.maven.wagon",
                        "wagon-provider-webdav", "1.0" ), 0, false );

        prb.withParent( parent );
        prb.withDependencies( papi, art );
        prb.withPlugins( jarp, comp );
        prb.withPlugins( comp );
        prb.withPluginDependencies( pdr );
        prb.withExtensions( wag );

        final EProjectDirectRelationships rels = prb.build();

        String json = mapper.writeValueAsString( rels );
        System.out.println( json );

        EProjectDirectRelationships clone = mapper.readValue(json, EProjectDirectRelationships.class);

        assertThat( clone.getSource(), equalTo( rels.getSource() ) );
        assertThat( clone.getProjectRef(), equalTo( rels.getProjectRef() ) );
        assertThat( clone.getParent(), equalTo( rels.getParent() ) );
        assertThat( clone.getBoms(), equalTo( rels.getBoms() ) );
        assertThat( clone.getDependencies(), equalTo( rels.getDependencies() ) );
        assertThat( clone.getPlugins(), equalTo( rels.getPlugins() ) );
        assertThat( clone.getPluginDependencies(), equalTo( rels.getPluginDependencies() ) );
        assertThat( clone.getExtensions(), equalTo( rels.getExtensions() ) );
        assertThat( clone.getManagedDependencies(), equalTo( rels.getManagedDependencies() ) );
        assertThat( clone.getManagedPlugins(), equalTo( rels.getManagedPlugins() ) );
    }
}
