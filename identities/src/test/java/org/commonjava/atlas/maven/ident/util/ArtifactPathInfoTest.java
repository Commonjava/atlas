/**
 * Copyright (C) 2012-2022 Red Hat, Inc. (nos-devel@redhat.com)
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.commonjava.atlas.maven.ident.version.part.SnapshotPart;
import org.junit.Test;

import java.text.SimpleDateFormat;

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
        String path = "/org/apache/commons/commons-lang3/3.0.0/commons-lang3-3.0.0-test.jar";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "3.0.0" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "test" ) );
        assertThat( pathInfo.getType(), equalTo( "jar" ) );

        path = "/org/apache/commons/commons-lang3/3.0.0/commons-lang3-3.0.0-test.tar.gz";
        pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "3.0.0" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "test" ) );
        assertThat( pathInfo.getType(), equalTo( "tar.gz" ) );
    }

    @Test
    public void matchNormalClassifier2()
    {
        String path = "/org/jboss/modules/jboss-modules/1.5.0.Final-temporary-redhat-00033/jboss-modules-1.5.0.Final-temporary-redhat-00033-project-sources.tar.gz";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "1.5.0.Final-temporary-redhat-00033" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "project-sources" ) );
        assertThat( pathInfo.getType(), equalTo( "tar.gz" ) );
    }

    @Test
    public void matchGAWithClassifier()
    {
        String path = "/org/apache/commons/commons-lang3/3.0.0.GA/commons-lang3-3.0.0.GA-test.jar";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "3.0.0.GA" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "test" ) );
        assertThat( pathInfo.getType(), equalTo( "jar" ) );

        path = "/org/apache/commons/commons-lang3/3.0.0.GA/commons-lang3-3.0.0.GA-test.tar.gz";
        pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "3.0.0.GA" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "test" ) );
        assertThat( pathInfo.getType(), equalTo( "tar.gz" ) );
    }

    @Test
    public void matchClassifierWithDot()
    {
        String path =
                "/org/uberfire/showcase-distribution-wars/7.33.0.Final-redhat-00003/showcase-distribution-wars-7.33.0.Final-redhat-00003-wildfly8.1.war";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getClassifier(), equalTo( "wildfly8.1" ) );
        assertThat( pathInfo.getType(), equalTo( "war" ) );

        path =
                "/org/uberfire/showcase-distribution-wars/7.33.0.Final-redhat-00003/showcase-distribution-wars-7.33.0.Final-redhat-00003-wildfly8.2.3.0.tar.gz";
        pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getClassifier(), equalTo( "wildfly8.2.3.0" ) );
        assertThat( pathInfo.getType(), equalTo( "tar.gz" ) );

        path =
                "/org/uberfire/showcase-distribution-wars/7.33.0.Final-redhat-00003/showcase-distribution-wars-7.33.0.Final-redhat-00003-wildfly.8.2.3.0.tar.gz";
        pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getClassifier(), equalTo( "wildfly.8.2.3.0" ) );
        assertThat( pathInfo.getType(), equalTo( "tar.gz" ) );
    }

    @Test
    public void testSnapshotPath()
    {
        final String path = "/org/commonjava/maven/galley/galley-transport-httpclient/0.10.4-SNAPSHOT/galley-transport-httpclient-0.10.4-20160229.212037-2.pom";
        ArtifactPathInfo info = ArtifactPathInfo.parse( path );
        SnapshotPart snap = info.getSnapshotInfo();
        assertEquals( "0.10.4", info.getReleaseVersion() );
        assertTrue( snap.isRemoteSnapshot() );
        assertEquals( "0.10.4-20160229.212037-2", snap.getValue() );
        assertEquals( "0.10.4-20160229.212037-2", snap.getLiteral() );
        assertEquals( 2, snap.getBuildNumber() );
        assertEquals( "20160229", new SimpleDateFormat( "yyyyMMdd" ).format( snap.getTimestamp() ) );
    }

    @Test
    public void matchCompoundExtTypes1()
    {
        String path =
                "/com/github/jomrazek/jomrazek-empty/1.0.1.redhat-00010/jomrazek-empty-1.0.1.redhat-00010-src.tar.bz2";
        ArtifactPathInfo info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "1.0.1.redhat-00010" ) );
        assertThat( info.getClassifier(), equalTo( "src" ) );
        assertThat( info.getType(), equalTo( "tar.bz2" ) );

        path =
                "/io/quarkus/platform/quarkus-google-cloud-services-bom-quarkus-platform-descriptor/2.13.7.Final-redhat-00001/quarkus-google-cloud-services-bom-quarkus-platform-descriptor-2.13.7.Final-redhat-00001-2.13.7.Final-redhat-00001.json";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "2.13.7.Final-redhat-00001" ) );
        assertThat( info.getClassifier(), equalTo( "2.13.7.Final-redhat-00001" ) );
        assertThat( info.getType(), equalTo( "json" ) );

        path =
                "/org/apache/cxf/cxf-repository/3.2.7.fuse-750011-redhat-00001/cxf-repository-3.2.7.fuse-750011-redhat-00001.xml.gz";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "3.2.7.fuse-750011-redhat-00001" ) );
        assertThat( info.getClassifier(), equalTo( "" ) );
        assertThat( info.getType(), equalTo( "xml.gz" ) );

        path =
                "/org/apache/commons/commons-compress/1.26.0.temporary-redhat-00002/commons-compress-1.26.0.temporary-redhat-00002.spdx.rdf.xml";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "1.26.0.temporary-redhat-00002" ) );
        assertThat( info.getClassifier(), equalTo( "" ) );
        assertThat( info.getType(), equalTo( "spdx.rdf.xml" ) );


    }

    @Test
    public void matchCompoundExtTypes2(){
        System.setProperty("atlas.compoext.types", "a.b.c, x.y.z");

        String path =
                "/com/example/example-artifact/1.0.0.redhat-00001/example-artifact-1.0.0.redhat-00001-x.y.z.tar";
        ArtifactPathInfo info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "1.0.0.redhat-00001" ) );
        assertThat( info.getClassifier(), equalTo( "x.y.z" ) );
        assertThat( info.getType(), equalTo( "tar" ) );

        path =
                "/com/example/example-artifact/1.0.0.redhat-00001/example-artifact-1.0.0.redhat-00001.a.b.c";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "1.0.0.redhat-00001" ) );
        assertThat( info.getClassifier(), equalTo( "" ) );
        assertThat( info.getType(), equalTo( "a.b.c" ) );

        path =
                "/com/example/example-artifact/1.0.0.redhat-00001/example-artifact-1.0.0.redhat-00001-sources.a.b.c";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "1.0.0.redhat-00001" ) );
        assertThat( info.getClassifier(), equalTo( "sources" ) );
        assertThat( info.getType(), equalTo( "a.b.c" ) );
    }

    @Test
    public void testChecksumTypes()
    {
        String path =
                "/com/webauthn4j/webauthn4j-test/0.12.0.RELEASE-redhat-00002/webauthn4j-test-0.12.0.RELEASE-redhat-00002-sources.jar.md5";
        ArtifactPathInfo info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "0.12.0.RELEASE-redhat-00002" ) );
        assertThat( info.getClassifier(), equalTo( "sources" ) );
        assertThat( info.getType(), equalTo( "jar.md5" ) );

        path =
                "com/github/jomrazek/jomrazek-empty/1.0.1.redhat-00010/jomrazek-empty-1.0.1.redhat-00010-src.tar.bz2.sha1";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "1.0.1.redhat-00010" ) );
        assertThat( info.getClassifier(), equalTo( "src" ) );
        assertThat( info.getType(), equalTo( "tar.bz2.sha1" ) );

        path = "/org/apache/commons/commons-lang3/3.0.0.GA/commons-lang3-3.0.0.GA-test.tar.gz.sha128";
        info = ArtifactPathInfo.parse( path );
        assertThat( info.getVersion(), equalTo( "3.0.0.GA" ) );
        assertThat( info.getClassifier(), equalTo( "test" ) );
        assertThat( info.getType(), equalTo( "tar.gz.sha128" ) );

        path =
                "/org/jboss/modules/jboss-modules/1.5.0.Final-temporary-redhat-00033/jboss-modules-1.5.0.Final-temporary-redhat-00033-project-sources.tar.gz.sha256";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getVersion(), equalTo( "1.5.0.Final-temporary-redhat-00033" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "project-sources" ) );
        assertThat( pathInfo.getType(), equalTo( "tar.gz.sha256" ) );

        path =
                "/com/webauthn4j/webauthn4j-test/0.12.0.RELEASE-redhat-00002/webauthn4j-test-0.12.0.RELEASE-redhat-00002-sources.jar.sha512";
        pathInfo = ArtifactPathInfo.parse( path );
        assertThat( pathInfo.getGroupId(), equalTo( "com.webauthn4j" ) );
        assertThat( pathInfo.getArtifactId(), equalTo( "webauthn4j-test" ) );
        assertThat( pathInfo.getVersion(), equalTo( "0.12.0.RELEASE-redhat-00002" ) );
        assertThat( pathInfo.getClassifier(), equalTo( "sources" ) );
        assertThat( pathInfo.getType(), equalTo( "jar.sha512" ) );
    }

}
