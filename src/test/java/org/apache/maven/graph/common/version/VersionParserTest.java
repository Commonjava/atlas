package org.apache.maven.graph.common.version;

import org.apache.maven.graph.common.version.parse.ParseException;
import org.apache.maven.graph.common.version.parse.VersionParser;
import org.junit.Test;

public class VersionParserTest
{

    @Test
    public void parseRangeWithoutStrings()
        throws ParseException
    {
        final String range = "[2.0.12,2.0.13]";
        final VersionSpec parsed = new VersionParser( range ).parse();

        System.out.println( parsed );
    }

    @Test
    public void parseRangeWithStrings()
        throws ParseException
    {
        final String range = "[2.0.12-redhat-1,2.0.12-redhat-2]";
        final VersionSpec parsed = new VersionParser( range ).parse();

        System.out.println( parsed );
    }

}
