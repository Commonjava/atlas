package org.commonjava.maven.atlas.graph.spi.jung;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.jung.JungEGraphDriver;
import org.commonjava.maven.atlas.tck.graph.GraphWorkspaceSPI_TCK;
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
