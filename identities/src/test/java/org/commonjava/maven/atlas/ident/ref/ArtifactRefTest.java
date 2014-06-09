package org.commonjava.maven.atlas.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ArtifactRefTest
{

    @Test( expected = InvalidRefException.class )
    public void failParsingGA()
    {
        ArtifactRef.parse( "org.foo:bar" );
    }

    @Test
    public void parseGAVIntoPOM()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String v = "1.0";

        final ArtifactRef ar = ArtifactRef.parse( String.format( "%s:%s:%s", g, a, v ) );

        assertThat( ar.getGroupId(), equalTo( g ) );
        assertThat( ar.getArtifactId(), equalTo( a ) );
        assertThat( ar.getVersionString(), equalTo( v ) );
        assertThat( ar.getType(), equalTo( "pom" ) );
        assertThat( ar.getClassifier(), nullValue() );
    }

    @Test
    public void parseGATV()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String v = "1.0";
        final String t = "zip";

        final ArtifactRef ar = ArtifactRef.parse( String.format( "%s:%s:%s:%s", g, a, t, v ) );

        assertThat( ar.getGroupId(), equalTo( g ) );
        assertThat( ar.getArtifactId(), equalTo( a ) );
        assertThat( ar.getVersionString(), equalTo( v ) );
        assertThat( ar.getType(), equalTo( t ) );
        assertThat( ar.getClassifier(), nullValue() );
    }

    @Test
    public void parseGATVC()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String v = "1.0";
        final String t = "zip";
        final String c = "sources";

        final ArtifactRef ar = ArtifactRef.parse( String.format( "%s:%s:%s:%s:%s", g, a, t, v, c ) );

        assertThat( ar.getGroupId(), equalTo( g ) );
        assertThat( ar.getArtifactId(), equalTo( a ) );
        assertThat( ar.getVersionString(), equalTo( v ) );
        assertThat( ar.getType(), equalTo( t ) );
        assertThat( ar.getClassifier(), equalTo( c ) );
    }

}
