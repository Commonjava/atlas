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
package org.commonjava.atlas.maven.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleVersionlessArtifactRef;
import org.commonjava.atlas.maven.ident.ref.VersionlessArtifactRef;
import org.junit.Test;

public class VersionlessArtifactRefTest
{

    @Test
    public void parseGA()
    {
        final String g = "org.foo";
        final String a = "bar";
        final VersionlessArtifactRef var = SimpleVersionlessArtifactRef.parse( String.format( "%s:%s", g, a ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( "pom" ) );
        assertThat( var.getClassifier(), nullValue() );
    }

    @Test
    public void parseGAT()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String t = "zip";

        final VersionlessArtifactRef var = SimpleVersionlessArtifactRef.parse( String.format( "%s:%s:%s", g, a, t ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( t ) );
        assertThat( var.getClassifier(), nullValue() );
    }

    @Test
    public void parseGATC()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String t = "zip";
        final String c = "sources";

        final VersionlessArtifactRef var = SimpleVersionlessArtifactRef.parse(
                String.format( "%s:%s:%s:%s", g, a, t, c ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( t ) );
        assertThat( var.getClassifier(), equalTo( c ) );
    }

    @Test
    public void parseGATVC_VersionDiscarded()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String t = "zip";
        final String v = "1.0";
        final String c = "sources";

        final VersionlessArtifactRef var =
            SimpleVersionlessArtifactRef.parse( String.format( "%s:%s:%s:%s:%s", g, a, t, v, c ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( t ) );
        assertThat( var.getClassifier(), equalTo( c ) );
    }

    @Test
    public void parseGATV_MistakeForGATC()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String t = "zip";
        final String v = "1.0";

        final VersionlessArtifactRef var = SimpleVersionlessArtifactRef.parse(
                String.format( "%s:%s:%s:%s", g, a, t, v ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( t ) );
        assertThat( var.getClassifier(), equalTo( v ) );
    }

    @Test
    public void parseGATVC()
    {
        final String g = "org.foo";
        final String a = "bar";
        final VersionlessArtifactRef var = SimpleVersionlessArtifactRef.parse( String.format( "%s:%s", g, a ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( "pom" ) );
        assertThat( var.getClassifier(), nullValue() );
    }

    @Test
    public void twoIdenticalArtifactsWrappedInVersionlessInstanceAreEqual_DefaultTypeAndClassifier()
    {
        final ProjectVersionRef pvr = new SimpleProjectVersionRef( "group", "artifact", "1" );
        final ArtifactRef r1 = new SimpleArtifactRef( pvr, null, null );
        final ArtifactRef r2 = new SimpleArtifactRef( pvr, null, null );

        final VersionlessArtifactRef vr1 = new SimpleVersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new SimpleVersionlessArtifactRef( r2 );

        assertThat( vr1, equalTo( vr2 ) );
        assertThat( vr1.hashCode(), equalTo( vr2.hashCode() ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreNotEqualWhenTypeDiffers()
    {
        final ProjectVersionRef pvr = new SimpleProjectVersionRef( "group", "artifact", "1" );
        final ArtifactRef r1 = new SimpleArtifactRef( pvr, "jar", null );
        final ArtifactRef r2 = new SimpleArtifactRef( pvr, "pom", null );

        final VersionlessArtifactRef vr1 = new SimpleVersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new SimpleVersionlessArtifactRef( r2 );

        assertThat( vr1.equals( vr2 ), equalTo( false ) );
        assertThat( vr1.hashCode() == vr2.hashCode(), equalTo( false ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreNotEqualWhenClassifierDiffers()
    {
        final ProjectVersionRef pvr = new SimpleProjectVersionRef( "group", "artifact", "1" );
        final ArtifactRef r1 = new SimpleArtifactRef( pvr, "jar", null );
        final ArtifactRef r2 = new SimpleArtifactRef( pvr, "jar", "foo" );

        final VersionlessArtifactRef vr1 = new SimpleVersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new SimpleVersionlessArtifactRef( r2 );

        assertThat( vr1.equals( vr2 ), equalTo( false ) );
        assertThat( vr1.hashCode() == vr2.hashCode(), equalTo( false ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreEqualWhenVersionDiffers()
    {
        final ProjectVersionRef pvr1 = new SimpleProjectVersionRef( "group", "artifact", "1" );
        final ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "group", "artifact", "2" );
        final ArtifactRef r1 = new SimpleArtifactRef( pvr1, null, null );
        final ArtifactRef r2 = new SimpleArtifactRef( pvr2, null, null );

        final VersionlessArtifactRef vr1 = new SimpleVersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new SimpleVersionlessArtifactRef( r2 );

        assertThat( vr1, equalTo( vr2 ) );
        assertThat( vr1.hashCode(), equalTo( vr2.hashCode() ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreEqualWhenVersionDiffersWithRange()
    {
        final ProjectVersionRef pvr1 = new SimpleProjectVersionRef( "group", "artifact", "1" );
        final ProjectVersionRef pvr2 = new SimpleProjectVersionRef( "group", "artifact", "[2,3.1]" );
        final ArtifactRef r1 = new SimpleArtifactRef( pvr1, null, null );
        final ArtifactRef r2 = new SimpleArtifactRef( pvr2, null, null );

        // trigger parsing.
        r1.getVersionSpec();
        r2.getVersionSpec();

        final VersionlessArtifactRef vr1 = new SimpleVersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new SimpleVersionlessArtifactRef( r2 );

        assertThat( vr1, equalTo( vr2 ) );
        assertThat( vr1.hashCode(), equalTo( vr2.hashCode() ) );
    }

}
