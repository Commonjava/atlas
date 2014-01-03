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
package org.commonjava.maven.atlas.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.junit.Test;

public class VersionlessArtifactRefTest
{
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
