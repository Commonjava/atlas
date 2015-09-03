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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.NeoSpecificProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 8/28/15.
 */
public class NeoIdentitiesTest
{

    private static final String WORKSPACE = "workspace";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private GraphDatabaseService graph;

    @Before
    public void before()
            throws Exception
    {
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
    public void projectRefCrossImplEquality()
    {
        ProjectRef aref = new SimpleProjectRef( "org.foo", "bar" );
        Node node = toNode( new SimpleProjectVersionRef( aref.getGroupId(), aref.getArtifactId(), "1" ) );

        NeoProjectRef naref = new NeoProjectRef( node );

        assertThat( naref, equalTo( aref ) );
    }

    @Test
    public void projectVersionRefCrossImplEquality()
    {
        ProjectVersionRef aref = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        Node node = toNode( aref );

        NeoProjectVersionRef naref = new NeoProjectVersionRef( node );

        assertThat( naref, equalTo( aref ) );
    }

    @Test
    public void artifactRefCrossImplEquality()
    {
        ProjectVersionRef pvr = new SimpleProjectVersionRef( "org.foo", "bar", "1" );
        ArtifactRef aref = new SimpleArtifactRef( pvr, "jar", null, false );
        Node node = toNode( aref );

        NeoArtifactRef naref = new NeoArtifactRef( node );

        assertThat( naref, equalTo( aref ) );
    }

    private Node toNode( ProjectVersionRef pvr )
    {
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

        return node;
    }
}
