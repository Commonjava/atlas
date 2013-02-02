package org.commonjava.maven.atlas.spi.neo4j;

import org.apache.log4j.Level;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.fixture.MemoryDriverFixture;
import org.commonjava.maven.atlas.tck.effective.EGraphDriverTCK;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;

public class MemoryDriverTest
    extends EGraphDriverTCK
{
    @Rule
    public MemoryDriverFixture fixture = new MemoryDriverFixture();

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Override
    protected EGraphDriver newDriverInstance()
        throws Exception
    {
        return fixture.newDriverInstance();
    }
}
