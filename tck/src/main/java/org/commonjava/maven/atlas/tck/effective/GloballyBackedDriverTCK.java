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
package org.commonjava.maven.atlas.tck.effective;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.junit.Test;

public abstract class GloballyBackedDriverTCK
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
