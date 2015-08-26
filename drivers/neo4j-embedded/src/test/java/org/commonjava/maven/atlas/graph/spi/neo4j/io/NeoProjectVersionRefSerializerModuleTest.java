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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JGraphConnection;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoArtifactRef;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoProjectRef;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoProjectVersionRef;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleTypeAndClassifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GA;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toNodeProperties;

/**
 * Created by jdcasey on 8/26/15.
 */
public class NeoProjectVersionRefSerializerModuleTest
{
    private static final String WORKSPACE = "workspace";

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
                                NeoSpecificProjectVersionRefSerializerModule.INSTANCE );

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
    public void roundTripArtifactRef()
            throws IOException
    {
        ArtifactRef aref = new SimpleArtifactRef( new SimpleProjectVersionRef( "org.foo", "bar", "1" ),
                                                        new SimpleTypeAndClassifier( "jar", null ), false );

        String json = mapper.writeValueAsString( aref );

        Transaction tx = graph.beginTx();
        Node node;
        try
        {
            node = graph.createNode();
            Conversions.toNodeProperties( aref, node, true );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        NeoArtifactRef naref = new NeoArtifactRef( node );
        String njson = mapper.writeValueAsString( naref );

        assertThat( njson, equalTo( json ) );

        ArtifactRef deser = mapper.readValue( njson, ArtifactRef.class );
        assertThat( deser, equalTo( aref ) );
    }

    @Test
    public void roundTripProjectVersionRef()
            throws IOException
    {
        ProjectVersionRef ref = new SimpleProjectVersionRef( "org.foo", "bar", "1" );

        String json = mapper.writeValueAsString( ref );

        Transaction tx = graph.beginTx();
        Node node;
        try
        {
            node = graph.createNode();
            Conversions.toNodeProperties( ref, node, true );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        NeoProjectVersionRef naref = new NeoProjectVersionRef( node );
        String njson = mapper.writeValueAsString( naref );

        assertThat( njson, equalTo( json ) );

        ProjectVersionRef deser = mapper.readValue( njson, ProjectVersionRef.class );
        assertThat( deser, equalTo( ref ) );
    }

    @Test
    public void roundTripProjectRef()
            throws IOException
    {
        ProjectRef aref = new SimpleProjectRef( "org.foo", "bar" );
        ProjectVersionRef pvr = new SimpleProjectVersionRef( aref.getGroupId(), aref.getArtifactId(), "1" );

        String json = mapper.writeValueAsString( aref );

        Transaction tx = graph.beginTx();
        Node node;
        try
        {
            node = graph.createNode();
            Conversions.toNodeProperties( pvr, node, true );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        NeoProjectRef naref = new NeoProjectRef( node );
        String njson = mapper.writeValueAsString( naref );

        assertThat( njson, equalTo( json ) );

        ProjectRef deser = mapper.readValue( njson, ProjectRef.class );
        assertThat( deser, equalTo( aref ) );
    }

}
