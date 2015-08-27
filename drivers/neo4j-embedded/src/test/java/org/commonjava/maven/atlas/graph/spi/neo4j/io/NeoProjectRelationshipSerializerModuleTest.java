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
package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.maven.atlas.graph.rel.BomRelationship;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleBomRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.graph.rel.SimplePluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimplePluginRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoBomRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoDependencyRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoExtensionRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoParentRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoPluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoPluginRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jdcasey on 8/26/15.
 */
public class NeoProjectRelationshipSerializerModuleTest
{
    private static final String WORKSPACE = "workspace";

    private static final URI TEST_URI;

    static
    {
        URI u;
        try
        {
            u = new URI( "test:location" );
        }
        catch ( URISyntaxException e )
        {
            Logger logger = LoggerFactory.getLogger( NeoProjectRelationshipSerializerModuleTest.class );
            logger.error( "Failed to construct test URI", e );
            u = null;
        }

        TEST_URI = u;
    }

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private ObjectMapper mapper;

    private GraphDatabaseService graph;

    @Before
    public void before()
            throws Exception
    {
        mapper = new ObjectMapper();
        mapper.registerModules( ProjectVersionRefSerializerModule.INSTANCE,
                                NeoSpecificProjectVersionRefSerializerModule.INSTANCE,
                                ProjectRelationshipSerializerModule.INSTANCE,
                                NeoSpecificProjectRelationshipSerializerModule.INSTANCE );

        mapper.setSerializationInclusion( JsonInclude.Include.NON_EMPTY );
        mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true );

        mapper.enable( SerializationFeature.INDENT_OUTPUT, SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID );

        mapper.enable( MapperFeature.AUTO_DETECT_FIELDS );
        //        disable( MapperFeature.AUTO_DETECT_GETTERS );

        mapper.disable( SerializationFeature.WRITE_NULL_MAP_VALUES, SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );

        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );

        graph = new GraphDatabaseFactory().newEmbeddedDatabase( temp.newFolder( "db" ).getAbsolutePath() );
    }

    @After
    public void after()
    {
        if ( graph != null )
        {
            graph.shutdown();
        }
    }

    @Test
    public void roundTripParentRelationship()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );

        ParentRelationship rel =
                new SimpleParentRelationship( pvr );

        String json = mapper.writeValueAsString( rel );

        Relationship r = store( rel );

        NeoParentRelationship result = new NeoParentRelationship( r );

        String njson = mapper.writeValueAsString( result );

        assertThat( njson, equalTo( json ) );

        ParentRelationship deser = mapper.readValue( njson, ParentRelationship.class );
        assertThat( deser, equalTo( rel ) );
    }

    @Test
    public void roundTripDependencyRelationship()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "org.foo", "dep", "1.0" );

        DependencyRelationship rel = new SimpleDependencyRelationship(TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, pvr2.asJarArtifact(),
                                                                            DependencyScope.compile, 0, false );

        String json = mapper.writeValueAsString( rel );

        Relationship r = store( rel );

        NeoDependencyRelationship result = new NeoDependencyRelationship( r );

        String njson = mapper.writeValueAsString( result );

        assertThat( njson, equalTo( json ) );

        DependencyRelationship deser = mapper.readValue( njson, DependencyRelationship.class );
        assertThat( deser, equalTo( rel ) );
    }

    private Relationship store( ProjectRelationship<?, ?> rel )
    {
        Relationship r;
        Transaction tx = graph.beginTx();
        try
        {
            Node start = graph.createNode();
            Conversions.toNodeProperties( rel.getDeclaring(), start, true );
            Node end = graph.createNode();
            Conversions.toNodeProperties( rel.getTarget(), end, false );

            r = start.createRelationshipTo( end, GraphRelType.map( rel.getType(), rel.isManaged() ) );
            Conversions.toRelationshipProperties( rel, r );

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return r;
    }

    @Test
    public void roundTripExtensionRelationship()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "org.foo", "dep", "1.0" );

        ExtensionRelationship rel = new SimpleExtensionRelationship(TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, pvr2, 0 );

        String json = mapper.writeValueAsString( rel );

        Relationship r = store( rel );

        NeoExtensionRelationship result = new NeoExtensionRelationship( r );

        String njson = mapper.writeValueAsString( result );

        assertThat( njson, equalTo( json ) );

        ExtensionRelationship deser = mapper.readValue( njson, ExtensionRelationship.class );
        assertThat( deser, equalTo( rel ) );
    }

    @Test
    public void roundTripBomRelationship()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "org.foo", "dep", "1.0" );

        BomRelationship rel = new SimpleBomRelationship(TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, pvr2, 0 );

        String json = mapper.writeValueAsString( rel );

        Relationship r = store( rel );

        NeoBomRelationship result = new NeoBomRelationship( r );

        String njson = mapper.writeValueAsString( result );

        assertThat( njson, equalTo( json ) );

        BomRelationship deser = mapper.readValue( njson, BomRelationship.class );
        assertThat( deser, equalTo( rel ) );
    }

    @Test
    public void roundTripPluginRelationship()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "org.foo", "dep", "1.0" );

        PluginRelationship rel = new SimplePluginRelationship(TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, pvr2, 0, false );

        String json = mapper.writeValueAsString( rel );

        Relationship r = store( rel );

        NeoPluginRelationship result = new NeoPluginRelationship( r );

        String njson = mapper.writeValueAsString( result );

        assertThat( njson, equalTo( json ) );

        PluginRelationship deser = mapper.readValue( njson, PluginRelationship.class );
        assertThat( deser, equalTo( rel ) );
    }

    @Test
    public void roundTripPluginDependencyRelationship()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "org.foo", "dep", "1.0" );
        ProjectRef pr = new SimpleProjectRef( "org.foo", "plugin" );

        PluginDependencyRelationship rel = new SimplePluginDependencyRelationship(TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, pr, pvr2.asJarArtifact(), 0, false );

        String json = mapper.writeValueAsString( rel );

        Relationship r = store( rel );

        NeoPluginDependencyRelationship result = new NeoPluginDependencyRelationship( r );

        String njson = mapper.writeValueAsString( result );

        assertThat( njson, equalTo( json ) );

        PluginDependencyRelationship deser = mapper.readValue( njson, PluginDependencyRelationship.class );
        assertThat( deser, equalTo( rel ) );
    }

    @Test
    public void roundTripMixedList()
            throws Exception
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ProjectVersionRef dep = new SimpleProjectVersionRef( "org.foo", "dep", "1.0" );
        ProjectVersionRef bom = new SimpleProjectVersionRef( "org.foo", "bom", "1.0" );
        ProjectVersionRef ext = new SimpleProjectVersionRef( "org.foo", "ext", "1.0" );
        ProjectVersionRef plug = new SimpleProjectVersionRef( "org.foo", "plug", "1.0" );
        ProjectVersionRef pdep = new SimpleProjectVersionRef( "org.foo", "pdep", "1.0" );
        ProjectVersionRef parent = new SimpleProjectVersionRef( "org.foo", "parent", "1.0" );
        ProjectRef pr = new SimpleProjectRef( "org.foo", "plugin" );

        List<ProjectRelationship<?, ?>> rels = Arrays.<ProjectRelationship<?, ?>> asList(
                new SimpleParentRelationship( TEST_URI, pvr, parent ),
                new SimpleDependencyRelationship( TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, dep.asJarArtifact(),
                                                  DependencyScope.compile, 0, false ),
                new SimpleBomRelationship( TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, ext, 0 ),
                new SimpleExtensionRelationship( TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, ext, 0 ),
                new SimplePluginRelationship( TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, plug, 0, false ),
                new SimplePluginDependencyRelationship( TEST_URI, RelationshipUtils.POM_ROOT_URI, pvr, pr,
                                                        pdep.asJarArtifact(), 0, false ) );

        String json = mapper.writeValueAsString( rels );

        List<Relationship> rs = new ArrayList<Relationship>();
        for ( ProjectRelationship<?, ?> rel : rels )
        {
            rs.add( store( rel ) );
        }

        int i=0;
        List<ProjectRelationship<?, ?>> nrels = Arrays.<ProjectRelationship<?, ?>>asList(
                new NeoParentRelationship( rs.get( i++ ) ),
                new NeoDependencyRelationship( rs.get(i++) ),
                new NeoBomRelationship( rs.get(i++) ),
                new NeoExtensionRelationship( rs.get(i++) ),
                new NeoPluginRelationship( rs.get(i++) ),
                new NeoPluginDependencyRelationship( rs.get(i++) )
        );

        String njson = mapper.writeValueAsString( nrels );

        assertThat( njson, equalTo( json ) );

        List<ProjectRelationship<?, ?>> deser = mapper.readValue( njson,
                                                                  new TypeReference<List<ProjectRelationship<?, ?>>>()
                                                                  {
                                                                  } );
        assertThat( deser, equalTo( rels ) );
    }

}
