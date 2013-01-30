/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.common.version.transform;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
        return getFormat().format( d );
    }

    public static Date parseSnapshotTimestamp( final String tstamp )
        throws ParseException
    {
        return getFormat().parse( tstamp );
    }

    private static DateFormat getFormat()
    {
        final SimpleDateFormat fmt = new SimpleDateFormat( SNAPSHOT_TSTAMP_FORMAT );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

        return fmt;
    }

}
