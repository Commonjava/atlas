/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
