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
package org.commonjava.maven.atlas.ident.version.transform;

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
