/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VersionlessArtifactRefTest
{

    @Test
    public void parseGA()
    {
        final String g = "org.foo";
        final String a = "bar";
        final VersionlessArtifactRef var = VersionlessArtifactRef.parse( String.format( "%s:%s", g, a ) );

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

        final VersionlessArtifactRef var = VersionlessArtifactRef.parse( String.format( "%s:%s:%s", g, a, t ) );

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

        final VersionlessArtifactRef var = VersionlessArtifactRef.parse( String.format( "%s:%s:%s:%s", g, a, t, c ) );

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
            VersionlessArtifactRef.parse( String.format( "%s:%s:%s:%s:%s", g, a, t, v, c ) );

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

        final VersionlessArtifactRef var = VersionlessArtifactRef.parse( String.format( "%s:%s:%s:%s", g, a, t, v ) );

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
        final VersionlessArtifactRef var = VersionlessArtifactRef.parse( String.format( "%s:%s", g, a ) );

        assertThat( var.getGroupId(), equalTo( g ) );
        assertThat( var.getArtifactId(), equalTo( a ) );
        assertThat( var.getType(), equalTo( "pom" ) );
        assertThat( var.getClassifier(), nullValue() );
    }

    @Test
    public void identicalVersionlessArtifactsAreNotEqualWhenOptionalFlagsDiffer()
    {
        // net.sf.kxml:kxml2:*:jar
        final String g = "net.sf.kxml";
        final String a = "kxml2";
        final String v = "1";
        final String t = "jar";

        final VersionlessArtifactRef r1 = new VersionlessArtifactRef( new ArtifactRef( g, a, v, t, null, false ) );
        final VersionlessArtifactRef r2 = new VersionlessArtifactRef( new ArtifactRef( g, a, v, t, null, true ) );

        assertThat( r1.equals( r2 ), equalTo( false ) );
        assertThat( r1.hashCode() == r2.hashCode(), equalTo( false ) );
    }

    @Test
    public void twoIdenticalArtifactsWrappedInVersionlessInstanceAreEqual_DefaultTypeAndClassifier()
    {
        final ProjectVersionRef pvr = new ProjectVersionRef( "group", "artifact", "1" );
        final ArtifactRef r1 = new ArtifactRef( pvr, null, null, false );
        final ArtifactRef r2 = new ArtifactRef( pvr, null, null, false );

        final VersionlessArtifactRef vr1 = new VersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new VersionlessArtifactRef( r2 );

        assertThat( vr1, equalTo( vr2 ) );
        assertThat( vr1.hashCode(), equalTo( vr2.hashCode() ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreNotEqualWhenTypeDiffers()
    {
        final ProjectVersionRef pvr = new ProjectVersionRef( "group", "artifact", "1" );
        final ArtifactRef r1 = new ArtifactRef( pvr, "jar", null, false );
        final ArtifactRef r2 = new ArtifactRef( pvr, "pom", null, false );

        final VersionlessArtifactRef vr1 = new VersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new VersionlessArtifactRef( r2 );

        assertThat( vr1.equals( vr2 ), equalTo( false ) );
        assertThat( vr1.hashCode() == vr2.hashCode(), equalTo( false ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreNotEqualWhenClassifierDiffers()
    {
        final ProjectVersionRef pvr = new ProjectVersionRef( "group", "artifact", "1" );
        final ArtifactRef r1 = new ArtifactRef( pvr, "jar", null, false );
        final ArtifactRef r2 = new ArtifactRef( pvr, "jar", "foo", false );

        final VersionlessArtifactRef vr1 = new VersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new VersionlessArtifactRef( r2 );

        assertThat( vr1.equals( vr2 ), equalTo( false ) );
        assertThat( vr1.hashCode() == vr2.hashCode(), equalTo( false ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreEqualWhenVersionDiffers()
    {
        final ProjectVersionRef pvr1 = new ProjectVersionRef( "group", "artifact", "1" );
        final ProjectVersionRef pvr2 = new ProjectVersionRef( "group", "artifact", "2" );
        final ArtifactRef r1 = new ArtifactRef( pvr1, null, null, false );
        final ArtifactRef r2 = new ArtifactRef( pvr2, null, null, false );

        final VersionlessArtifactRef vr1 = new VersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new VersionlessArtifactRef( r2 );

        assertThat( vr1, equalTo( vr2 ) );
        assertThat( vr1.hashCode(), equalTo( vr2.hashCode() ) );
    }

    @Test
    public void twoArtifactsWrappedInVersionlessInstanceAreEqualWhenVersionDiffersWithRange()
    {
        final ProjectVersionRef pvr1 = new ProjectVersionRef( "group", "artifact", "1" );
        final ProjectVersionRef pvr2 = new ProjectVersionRef( "group", "artifact", "[2,3.1]" );
        final ArtifactRef r1 = new ArtifactRef( pvr1, null, null, false );
        final ArtifactRef r2 = new ArtifactRef( pvr2, null, null, false );

        // trigger parsing.
        r1.getVersionSpec();
        r2.getVersionSpec();

        final VersionlessArtifactRef vr1 = new VersionlessArtifactRef( r1 );
        final VersionlessArtifactRef vr2 = new VersionlessArtifactRef( r2 );

        assertThat( vr1, equalTo( vr2 ) );
        assertThat( vr1.hashCode(), equalTo( vr2.hashCode() ) );
    }

}
