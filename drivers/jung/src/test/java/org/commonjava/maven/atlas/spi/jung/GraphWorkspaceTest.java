package org.commonjava.maven.atlas.spi.jung;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.effective.EGraphManager;
import org.commonjava.maven.atlas.spi.jung.effective.JungEGraphDriver;
import org.commonjava.maven.atlas.tck.effective.GraphWorkspaceSPI_TCK;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;

public class GraphWorkspaceTest
    extends GraphWorkspaceSPI_TCK
{
    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    private EGraphManager manager;

    @Override
    protected EGraphManager getManager()
        throws Exception
    {
        if ( manager == null )
        {
            manager = new EGraphManager( new JungEGraphDriver() );
        }

        return manager;
    }

}
