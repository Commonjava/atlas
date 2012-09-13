package org.apache.maven.graph.common.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

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

    @Test
    public void addTwoIdenticalRefsToASetAndVerifyThatOnlyOneIsAdded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1, equalTo( ref2 ) );

        final Set<ProjectVersionRef> set = new HashSet<ProjectVersionRef>();
        assertThat( set.add( ref1 ), equalTo( true ) );
        assertThat( set.add( ref2 ), equalTo( false ) );
    }

    @Test
    public void addTwoIdenticalCompoundRefsToASetAndVerifyThatOnlyOneIsAdded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "[1.1.1-baz-1,1.1.1-baz-2]" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "[1.1.1-baz-1,1.1.1-baz-2]" );

        assertThat( ref1, equalTo( ref2 ) );

        final Set<ProjectVersionRef> set = new HashSet<ProjectVersionRef>();
        assertThat( set.add( ref1 ), equalTo( true ) );
        assertThat( set.add( ref2 ), equalTo( false ) );
    }

}
