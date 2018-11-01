/**
 * Copyright (C) 2012 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.ident.version;

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
