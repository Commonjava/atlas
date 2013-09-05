package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.fixture.FileDriverFixture;
import org.commonjava.maven.atlas.tck.graph.GraphWorkspaceSPI_TCK;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;

public class FileGraphWorkspaceTest
    extends GraphWorkspaceSPI_TCK
{

    @Rule
    public FileDriverFixture fixture = new FileDriverFixture();

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Override
    protected synchronized EGraphManager getManager()
        throws Exception
    {
        return fixture.manager();
    }
}
