package org.commonjava.maven.atlas.spi.neo4j;

import org.apache.log4j.Level;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.fixture.FileDriverFixture;
import org.commonjava.maven.atlas.tck.effective.EGraphDriverTCK;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;

public class FileDriverTest
    extends EGraphDriverTCK
{
    @Rule
    public FileDriverFixture fixture = new FileDriverFixture();

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
