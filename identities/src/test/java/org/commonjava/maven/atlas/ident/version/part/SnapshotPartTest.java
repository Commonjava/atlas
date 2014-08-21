package org.commonjava.maven.atlas.ident.version.part;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.maven.atlas.ident.util.SnapshotUtils;
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

    @Test
    public void compareLocalVsRemoteSnapshot_LocalSortsLastAsMostRecent()
        throws Exception
    {
        final SnapshotPart first = new SnapshotPart( SnapshotUtils.LOCAL_SNAPSHOT_VERSION_PART );
        final SnapshotPart second = new SnapshotPart( "20140604.124350-1" );

        final int result = first.compareTo( second );

        assertThat( result, equalTo( 1 ) );
    }

}
