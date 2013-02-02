package org.commonjava.maven.atlas.tck.effective;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.junit.Test;

public abstract class EGraphDriverTCK
    extends AbstractSPI_TCK
{

    @Test
    public void childDriverIsDerivedFromParent()
        throws Exception
    {
        final EGraphDriver parent = newDriverInstance();
        final EGraphDriver child = parent.newInstance();

        assertThat( child.isDerivedFrom( parent ), equalTo( true ) );
    }

}
