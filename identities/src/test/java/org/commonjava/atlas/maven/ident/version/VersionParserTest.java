/**
 * Copyright (C) 2012 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.ident.version;

import org.commonjava.atlas.maven.ident.version.VersionSpec;
import org.commonjava.atlas.maven.ident.version.parse.VersionParser;
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

    @Test
    public void parseSingleVersionWithPlusSeparator()
                    throws Exception
    {
        final String version = "9+181-r4173-1";
        final VersionSpec parsed = new VersionParser( version ).parse();

        System.out.println( parsed );
    }
}
