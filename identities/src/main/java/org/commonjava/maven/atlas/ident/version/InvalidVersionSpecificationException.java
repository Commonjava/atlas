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

import java.util.IllegalFormatException;

public class InvalidVersionSpecificationException
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final Object[] params;

    private String version;

    public InvalidVersionSpecificationException( final String version, final String message, final Throwable cause,
                                                 final Object... params )
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
