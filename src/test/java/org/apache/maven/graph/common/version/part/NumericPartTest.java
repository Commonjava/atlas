package org.apache.maven.graph.common.version.part;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.junit.Test;

public class NumericPartTest
{

    @Test
    public void largeNumericVersionsEqual()
        throws InvalidVersionSpecificationException
    {
        assertThat( new NumericPart( "20050331" ), equalTo( new NumericPart( "20050331" ) ) );
    }

}
