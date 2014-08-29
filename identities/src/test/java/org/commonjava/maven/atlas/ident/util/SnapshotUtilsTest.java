package org.commonjava.maven.atlas.ident.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class SnapshotUtilsTest
{

    @Test
    public void roundTripSnapshotTimestamp_StartWithString()
        throws ParseException
    {
        final String tstamp = "20140828.225831";

        System.out.println( tstamp );

        final Date d = SnapshotUtils.parseSnapshotTimestamp( tstamp );

        final String result = SnapshotUtils.generateSnapshotTimestamp( d );
        System.out.println( result );

        assertThat( result, equalTo( tstamp ) );
    }

    @Test
    public void roundTripSnapshotTimestamp()
        throws ParseException
    {
        final Date d = SnapshotUtils.getCurrentTimestamp();

        System.out.println( d );
        final String tstamp = SnapshotUtils.generateSnapshotTimestamp( d );
        System.out.println( tstamp );

        final Date result = SnapshotUtils.parseSnapshotTimestamp( tstamp );
        System.out.println( result );

        assertThat( result, equalTo( d ) );
    }

    @Test
    public void roundTripLastUpdatedTimestamp()
        throws ParseException
    {
        final Date d = SnapshotUtils.getCurrentTimestamp();

        System.out.println( d );
        final String tstamp = SnapshotUtils.generateUpdateTimestamp( d );
        System.out.println( tstamp );

        final Date result = SnapshotUtils.parseUpdateTimestamp( tstamp );
        System.out.println( result );

        assertThat( result, equalTo( d ) );
    }

}
