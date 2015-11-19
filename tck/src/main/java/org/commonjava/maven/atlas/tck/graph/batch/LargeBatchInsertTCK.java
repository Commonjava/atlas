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
package org.commonjava.maven.atlas.tck.graph.batch;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class LargeBatchInsertTCK
    extends AbstractSPI_TCK
{

    private static final String NAMECHARS = "abcdefghijklmnopqrstuvwxyz";

    private Random random = new Random();

    @Test
    public void run()
        throws Exception
    {
        final ProjectVersionRef myRef = new SimpleProjectVersionRef( "my.group", "my-artifact", "1.0" );
        List<DependencyRelationship> rels = new ArrayList<DependencyRelationship>();
        for ( int i = 0; i < 550; i++ )
        {
            rels.add(new SimpleDependencyRelationship( sourceURI(), RelationshipUtils.POM_ROOT_URI, myRef, newArtifact(),
                                                       DependencyScope.compile, i, false, false ) );
        }

        final RelationshipGraph graph = simpleGraph( myRef );

        graph.storeRelationships( rels );

        Set<ProjectRelationship<?, ?>> result = graph.getAllRelationships();
        assertThat( result.size(), equalTo( rels.size() ) );

        for ( ProjectRelationship<?, ?> rel: rels )
        {
            assertThat( rel + " was not returned from the graph!", result.contains( rel ), equalTo( true ) );
        }
    }

    private ArtifactRef newArtifact()
    {
        return new SimpleArtifactRef( genName(), genName(), genNum(), "jar", null, false );
    }

    private String genName()
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < 10; i++ )
        {
            sb.append(NAMECHARS.charAt( Math.abs( random.nextInt() ) % NAMECHARS.length() ) );
        }

        return sb.toString();
    }

    private String genNum()
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < 4; i++ )
        {
            sb.append(Integer.toString(Math.abs( random.nextInt() ) % 10 ) );
        }

        return sb.toString();
    }
}
