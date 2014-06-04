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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.maven.atlas.ident.version.part.SnapshotPart;

public class SnapshotSupport
{

    public static final String SNAPSHOT_TSTAMP_FORMAT = "yyyyMMdd.hhmmss";

    public static final String REMOTE_SNAPSHOT_PART_PATTERN = "^((.+)-)?([0-9]{8}.[0-9]{6})-([0-9]+)$";

    public static String generateSnapshotSuffix( final Date d, final int buildNumber )
    {
        return generateSnapshotTimestamp( d ) + "-" + buildNumber;
    }

    public static String generateSnapshotTimestamp( final Date d )
    {
        return getFormat().format( d );
    }

    public static boolean isRemoteSnapshotVersionPart( final String literal )
    {
        return literal.matches( REMOTE_SNAPSHOT_PART_PATTERN );
    }

    public static SnapshotPart parseRemoteSnapshotVersionPart( final String literal )
    {
        final Pattern pattern = Pattern.compile( REMOTE_SNAPSHOT_PART_PATTERN );
        final Matcher matcher = pattern.matcher( literal );
        if ( matcher.matches() )
        {
            final String tstamp = matcher.group( 3 );
            final String bn = matcher.group( 4 );
            if ( tstamp != null || bn != null )
            {
                Date d;
                try
                {
                    d = parseSnapshotTimestamp( tstamp );
                }
                catch ( final ParseException e )
                {
                    throw new IllegalArgumentException( "'" + literal
                        + "' is not a remote snapshot version-part (of the format: " + SNAPSHOT_TSTAMP_FORMAT
                        + "-NN (invalid timestamp)", e );
                }
                final int build = Integer.parseInt( bn );

                return new SnapshotPart( d, build, literal );
            }
        }

        throw new IllegalArgumentException( "'" + literal + "' is not a remote snapshot version-part (of the format: "
            + SNAPSHOT_TSTAMP_FORMAT + "-NN" );
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
