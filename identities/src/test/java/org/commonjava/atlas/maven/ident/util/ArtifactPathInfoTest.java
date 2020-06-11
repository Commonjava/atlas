/**
 * Copyright (C) 2012 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.ident.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.junit.Test;

public class ArtifactPathInfoTest
{

    @Test
    public void matchSnapshotUIDVersion()
    {
        final String path =
            "/path/to/unsigner-maven-plugin/0.2-SNAPSHOT/unsigner-maven-plugin-0.2-20120307.200227-1.jar";
        assertThat( ArtifactPathInfo.parse( path )
                                    .isSnapshot(), equalTo( true ) );
    }

    @Test
    public void matchSnapshotNonUIDVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin/0.2-SNAPSHOT/unsigner-maven-plugin-0.2-SNAPSHOT.jar";
        assertThat( ArtifactPathInfo.parse( path )
                                    .isSnapshot(), equalTo( true ) );
    }

    @Test
    public void dontMatchNonSnapshotVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin/0.2/unsigner-maven-plugin-0.2.jar";
        assertThat( ArtifactPathInfo.parse( path )
                                    .isSnapshot(), equalTo( false ) );
    }

    @Test
    public void matchNormalClassifier()
    {
        final String path = "/org/apache/commons/commons-lang3/3.0.0/commons-lang3-3.0.0-test.jar";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "3.0.0" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "test" ) );
        assertThat( pathInfo.getType(), equalTo( "jar" ) );
    }

    @Test
    public void matchGAWithClassifier()
    {
        final String path = "/org/apache/commons/commons-lang3/3.0.0.GA/commons-lang3-3.0.0.GA-test.jar";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "3.0.0.GA" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "test" ) );
        assertThat( pathInfo.getType(), equalTo( "jar" ) );
    }

    @Test
    public void matchClassifierWithDot()
    {
        final String path =
                "/org/uberfire/showcase-distribution-wars/7.33.0.Final-redhat-00003/showcase-distribution-wars-7.33.0.Final-redhat-00003-wildfly8.1.war";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getClassifier(), equalTo( "wildfly8.1" ) );
        assertThat( pathInfo.getType(), equalTo( "war" ) );
    }

}
