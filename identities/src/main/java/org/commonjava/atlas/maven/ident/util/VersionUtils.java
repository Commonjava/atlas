/**
 * Copyright (C) 2012-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.ident.util;

import org.commonjava.atlas.maven.ident.version.InvalidVersionSpecificationException;
import org.commonjava.atlas.maven.ident.version.RangeVersionSpec;
import org.commonjava.atlas.maven.ident.version.SingleVersion;
import org.commonjava.atlas.maven.ident.version.VersionSpec;
import org.commonjava.atlas.maven.ident.version.parse.ParseException;
import org.commonjava.atlas.maven.ident.version.parse.TokenMgrError;
import org.commonjava.atlas.maven.ident.version.parse.VersionParser;

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

    public static boolean isValidSingleVersion( final String version )
    {
        boolean result = true;
        
        if ( version == null || version.trim().length() < 1 )
        {
            return false;
        }
        try
        {
            SingleVersion singleVersion = new VersionParser( version ).single();
        }
        catch ( final ParseException e )
        {
            result = false;
        }
        catch ( final TokenMgrError e )
        {
            result = false;
        }
        return result;
    }

}
