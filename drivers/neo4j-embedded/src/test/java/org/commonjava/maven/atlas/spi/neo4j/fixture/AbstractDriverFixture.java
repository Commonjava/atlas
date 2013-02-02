package org.commonjava.maven.atlas.spi.neo4j.fixture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.util.logging.Logger;
import org.junit.rules.ExternalResource;

public abstract class AbstractDriverFixture
    extends ExternalResource
{

    private final List<EGraphDriver> drivers = new ArrayList<EGraphDriver>();

    public EGraphDriver newDriverInstance()
        throws Exception
    {
        final EGraphDriver driver = create();
        drivers.add( driver );

        return driver;
    }

    protected abstract EGraphDriver create()
        throws Exception;

    @Override
    protected void after()
    {
        for ( final EGraphDriver driver : drivers )
        {
            try
            {
                driver.close();
            }
            catch ( final IOException e )
            {
                new Logger( getClass() ).error( "Failed to close driver: %s. Reason: %s", e, driver, e.getMessage() );
            }
        }
    }
}
