package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.fixture.FileDriverFixture;
import org.commonjava.maven.atlas.tck.graph.EGraphManagerTCK;
import org.junit.Rule;

public class FileEGraphManagerTest
    extends EGraphManagerTCK
{
    @Rule
    public FileDriverFixture fixture = new FileDriverFixture();

    @Override
    protected synchronized EGraphManager getManager()
        throws Exception
    {
        return fixture.manager();
    }
}
