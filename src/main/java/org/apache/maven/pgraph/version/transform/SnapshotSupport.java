package org.apache.maven.pgraph.version.transform;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SnapshotSupport
{

    public static final String SNAPSHOT_TSTAMP_FORMAT = "yyyyMMdd.hhmmss";

    public static final String SNAPSHOT_PATTERN = "^((.+)-)?([0-9]{8}.[0-9]{6})-([0-9]+)$";

    public static String generateSnapshotSuffix( final Date d, final int buildNumber )
    {
        return generateSnapshotTimestamp( d ) + "-" + buildNumber;
    }

    public static String generateSnapshotTimestamp( final Date d )
    {
        final Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
        cal.setTimeInMillis( d.getTime() );

        return new SimpleDateFormat( SNAPSHOT_TSTAMP_FORMAT ).format( cal.getTime() );
    }

    public static Date parseSnapshotTimestamp( final String tstamp )
        throws ParseException
    {
        return new SimpleDateFormat( SNAPSHOT_TSTAMP_FORMAT ).parse( tstamp );
    }

}
