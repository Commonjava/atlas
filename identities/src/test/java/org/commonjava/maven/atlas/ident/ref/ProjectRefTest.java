package org.commonjava.maven.atlas.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ProjectRefTest
{

    @Test
    public void matchesTotalWildcardGroupId()
    {
        final ProjectRef pattern = new ProjectRef( "*", "foo" );
        final ProjectRef test = new ProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesTotalWildcardArtifactId()
    {
        final ProjectRef pattern = new ProjectRef( "org.bar", "*" );
        final ProjectRef test = new ProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesTerminatingWildcardGroupId()
    {
        final ProjectRef pattern = new ProjectRef( "org.*", "foo" );
        final ProjectRef test = new ProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesTerminatingWildcardArtifactId()
    {
        final ProjectRef pattern = new ProjectRef( "org.bar", "fo*" );
        final ProjectRef test = new ProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesEmbeddedWildcardGroupId()
    {
        final ProjectRef pattern = new ProjectRef( "org.*r", "foo" );
        final ProjectRef test = new ProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesEmbeddedWildcardArtifactId()
    {
        final ProjectRef pattern = new ProjectRef( "org.bar", "f*o" );
        final ProjectRef test = new ProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

}
