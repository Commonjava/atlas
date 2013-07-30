/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.ident.version;

import org.commonjava.maven.atlas.common.version.parse.ParseException;
import org.commonjava.maven.atlas.common.version.parse.TokenMgrError;
import org.commonjava.maven.atlas.common.version.parse.VersionParser;

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
            final VersionSpec spec = new VersionParser( version ).parse();
            if ( spec == null )
            {
                throw new InvalidVersionSpecificationException( version, "Parsed VersionSpec is null." );
            }

            return spec;
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
        catch ( final TokenMgrError e )
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
        catch ( final TokenMgrError e )
        {
            throw new InvalidVersionSpecificationException( version, "Failed to parse single version: %s", e,
                                                            e.getMessage() );
        }
    }

}
