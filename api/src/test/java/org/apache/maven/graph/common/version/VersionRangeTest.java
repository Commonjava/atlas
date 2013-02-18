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
package org.apache.maven.graph.common.version;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VersionRangeTest
{

    @Test
    public void identialRangeEquality()
        throws InvalidVersionSpecificationException
    {
        final RangeVersionSpec r1 = VersionUtils.createRange( "[1.1.1-baz-1,1.1.1-baz-2]" );
        final RangeVersionSpec r2 = VersionUtils.createRange( "[1.1.1-baz-1,1.1.1-baz-2]" );

        assertThat( r1.renderStandard(), equalTo( r2.renderStandard() ) );
        assertThat( r1.hashCode(), equalTo( r2.hashCode() ) );
        assertThat( r1, equalTo( r2 ) );
        assertThat( r2, equalTo( r1 ) );
    }

}
