package org.commonjava.maven.atlas.ident.version.part;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SnapshotPartTest
{

    @Test
    public void compareTwoRemoteSnapshotsWithDifferentTimestamps_ParsedFromLiterals()
        throws Exception
    {
        final SnapshotPart first = new SnapshotPart( "20140604.124355-1" );
        final SnapshotPart second = new SnapshotPart( "20140604.124350-1" );

        final int result = first.compareTo( second );

        assertThat( result, equalTo( 1 ) );
    }

}
