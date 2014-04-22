/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
