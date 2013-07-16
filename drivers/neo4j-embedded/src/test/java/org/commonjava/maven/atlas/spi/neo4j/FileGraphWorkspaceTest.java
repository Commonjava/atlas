package org.commonjava.maven.atlas.spi.neo4j;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.effective.EGraphManager;
import org.commonjava.maven.atlas.spi.neo4j.fixture.FileDriverFixture;
import org.commonjava.maven.atlas.tck.effective.GraphWorkspaceSPI_TCK;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;

public class FileGraphWorkspaceTest
    extends GraphWorkspaceSPI_TCK
{

    @Rule
    public FileDriverFixture fixture = new FileDriverFixture();

    private EGraphManager manager;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Override
    protected synchronized EGraphManager getManager()
        throws Exception
    {
        if ( manager == null )
        {
            manager = new EGraphManager( fixture.newDriverInstance() );
        }

        return manager;
    }

}
