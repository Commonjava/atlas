/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.atlas.graph;

import java.util.IllegalFormatException;

public class RelationshipGraphException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    private final Object[] params;

    public RelationshipGraphException( final String message, final Throwable error, final Object... params )
    {
        super( message, error );
        this.params = params;
    }

    public RelationshipGraphException( final String message, final Object... params )
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
                final String formatted = String.format( message.replaceAll( "\\{\\}", "%s" ), params );
                message = formatted;
            }
            catch ( final IllegalFormatException e )
            {
            }
        }

        return message;
    }
}
