package org.apache.maven.graph.common.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.junit.Test;

public class ProjectVersionRefTest
{

    @Test
    public void hashCodeEquality()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1.hashCode(), equalTo( ref2.hashCode() ) );
    }

    @Test
    public void objectEquality()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1, equalTo( ref2 ) );
    }

}
