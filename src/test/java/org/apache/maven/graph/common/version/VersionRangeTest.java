package org.apache.maven.graph.common.version;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VersionRangeTest
{

    @Test
    public void identialRangeEquality()
        throws InvalidVersionSpecificationException
    {
        final RangeVersionSpec r1 = VersionUtils.createRange( "[1.1.1-baz-1,1.1.1-baz-2]" );
        final RangeVersionSpec r2 = VersionUtils.createRange( "[1.1.1-baz-1,1.1.1-baz-2]" );

        assertThat( r1.renderStandard(), equalTo( r2.renderStandard() ) );
        assertThat( r1.hashCode(), equalTo( r2.hashCode() ) );
        assertThat( r1, equalTo( r2 ) );
        assertThat( r2, equalTo( r1 ) );
    }

}
