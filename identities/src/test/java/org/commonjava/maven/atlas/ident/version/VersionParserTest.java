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
package org.commonjava.maven.atlas.ident.version;

import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.atlas.ident.version.parse.VersionParser;
import org.junit.Test;

public class VersionParserTest
{

    @Test
    public void parseRangeWithoutStrings()
        throws Exception
    {
        final String range = "[2.0.12,2.0.13]";
        final VersionSpec parsed = new VersionParser( range ).parse();

        System.out.println( parsed );
    }

    @Test
    public void parseRangeWithStrings()
        throws Exception
    {
        final String range = "[2.0.12-redhat-1,2.0.12-redhat-2]";
        final VersionSpec parsed = new VersionParser( range ).parse();

        System.out.println( parsed );
    }

    @Test
    public void parseSingleVersionWithBasicDateTimeFormat()
        throws Exception
    {
        final String version = "20031129.200437";
        final VersionSpec parsed = new VersionParser( version ).parse();

        System.out.println( parsed );
    }

    @Test
    public void parseSingleVersionWithModifiedDateTimeFormat()
        throws Exception
    {
        final String version = "20031129.200437j";
        final VersionSpec parsed = new VersionParser( version ).parse();

        System.out.println( parsed );
    }

    @Test
    public void parseSingleVersionWithExtendedDateTimeFormat()
        throws Exception
    {
        final String version = "20031129.200437-600";
        final VersionSpec parsed = new VersionParser( version ).parse();

        System.out.println( parsed );
    }

}
