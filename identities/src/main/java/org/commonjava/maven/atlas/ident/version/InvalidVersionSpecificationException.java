/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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

import java.util.IllegalFormatException;

public class InvalidVersionSpecificationException
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final Object[] params;

    private String version;

    public InvalidVersionSpecificationException( final String version, final String message, final Throwable cause, final Object... params )
    {
        super( "'" + version + "': " + message, cause );
        this.params = params;
    }

    public InvalidVersionSpecificationException( final String version, final String message, final Object... params )
    {
        super( "'" + version + "': " + message );
        this.version = version;
        this.params = params;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public String getLocalizedMessage()
    {
        return getMessage();
    }

    @Override
    public String getMessage()
    {
        String format = super.getMessage();
        if ( params != null && params.length > 0 )
        {
            try
            {
                format = String.format( format, params );
            }
            catch ( final IllegalFormatException e )
            {
            }
        }

        return format;
    }
}
