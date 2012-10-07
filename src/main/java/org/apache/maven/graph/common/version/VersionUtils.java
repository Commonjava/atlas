package org.apache.maven.graph.common.version;

import org.apache.maven.graph.common.version.parse.ParseException;
import org.apache.maven.graph.common.version.parse.TokenMgrError;
import org.apache.maven.graph.common.version.parse.VersionParser;

public final class VersionUtils
{

    private VersionUtils()
    {
    }

    public static VersionSpec createFromSpec( final String version )
        throws InvalidVersionSpecificationException
    {
        checkEmpty( version );

        try
        {
            return new VersionParser( version ).parse();
        }
        catch ( final ParseException e )
        {
            throw new InvalidVersionSpecificationException( version, "Failed to parse version: %s", e, e.getMessage() );
        }
        catch ( final TokenMgrError e )
        {
            throw new InvalidVersionSpecificationException( version, "Failed to parse version: %s", e, e.getMessage() );
        }
    }

    private static void checkEmpty( final String version )
        throws InvalidVersionSpecificationException
    {
        if ( version == null || version.trim()
                                       .length() < 1 )
        {
            throw new InvalidVersionSpecificationException( version, "Valid versions cannot be null or empty" );
        }

    }

    public static RangeVersionSpec createRange( final String version )
        throws InvalidVersionSpecificationException
    {
        checkEmpty( version );

        try
        {
            return new VersionParser( version ).range();
        }
        catch ( final ParseException e )
        {
            throw new InvalidVersionSpecificationException( version, "Failed to parse version range: %s", e,
                                                            e.getMessage() );
        }
    }

    public static SingleVersion createSingleVersion( final String version )
        throws InvalidVersionSpecificationException
    {
        checkEmpty( version );

        try
        {
            return new VersionParser( version ).single();
        }
        catch ( final ParseException e )
        {
            throw new InvalidVersionSpecificationException( version, "Failed to parse single version: %s", e,
                                                            e.getMessage() );
        }
    }

}
