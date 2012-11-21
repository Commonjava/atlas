package org.apache.maven.graph.common.version;

import org.junit.Test;

public class VersionUtilsTest
{

    @Test
    public void createSingleTimestampVersionFormat()
        throws Exception
    {
        final String spec = "20031129.200437";
        final SingleVersion version = VersionUtils.createSingleVersion( spec );

        System.out.println( version );
    }

}
