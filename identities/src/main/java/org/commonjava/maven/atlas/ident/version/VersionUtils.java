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
package org.commonjava.maven.atlas.ident.version;

import org.commonjava.maven.atlas.ident.version.parse.ParseException;
import org.commonjava.maven.atlas.ident.version.parse.TokenMgrError;
import org.commonjava.maven.atlas.ident.version.parse.VersionParser;

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
