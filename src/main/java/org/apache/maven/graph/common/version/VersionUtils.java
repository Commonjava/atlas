package org.apache.maven.graph.common.version;

import org.apache.maven.graph.common.version.parse.ParseException;
import org.apache.maven.graph.common.version.parse.VersionParser;

public final class VersionUtils
{

    private VersionUtils()
    {
    }

    public static VersionSpec createFromSpec( final String version )
        throws InvalidVersionSpecificationException
    {
        try
        {
            return new VersionParser( version ).parse();
        }
        catch ( final ParseException e )
        {
            throw new InvalidVersionSpecificationException( version, "Failed to parse version: %s", e, e.getMessage() );
        }
    }

    public static RangeVersionSpec createRange( final String version )
        throws InvalidVersionSpecificationException
    {
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
