package org.apache.maven.graph.common.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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
