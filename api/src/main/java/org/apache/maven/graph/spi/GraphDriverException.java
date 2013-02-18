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
package org.apache.maven.graph.spi;

import java.util.IllegalFormatException;

public class GraphDriverException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    private final Object[] params;

    public GraphDriverException( final String message, final Throwable error, final Object... params )
    {
        super( message, error );
        this.params = params;
    }

    public GraphDriverException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    @Override
    public String getLocalizedMessage()
    {
        return getMessage();
    }

    @Override
    public String getMessage()
    {
        String message = super.getMessage();
        if ( params != null && params.length > 0 )
        {
            try
            {
                final String formatted = String.format( message, params );
                message = formatted;
            }
            catch ( final IllegalFormatException e )
            {
            }
        }

        return message;
    }
}
