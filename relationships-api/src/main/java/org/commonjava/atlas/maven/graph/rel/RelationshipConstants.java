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
package org.commonjava.atlas.maven.graph.rel;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by ruhan on 12/6/16.
 */
public class RelationshipConstants {

    public static final URI UNKNOWN_SOURCE_URI;

    public static final URI POM_ROOT_URI;

    public static final URI ANY_SOURCE_URI;

    public static final URI TERMINAL_PARENT_SOURCE_URI;

    static
    {
        final String uri = "atlas:terminal-parent";
        try
        {
            TERMINAL_PARENT_SOURCE_URI = new URI( uri );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Terminal-parent source URI constant is invalid: " + uri, e );
        }

        try
        {
            ANY_SOURCE_URI = new URI( "any:any" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct any-source URI: 'any:any'" );
        }

        try
        {
            UNKNOWN_SOURCE_URI = new URI( "unknown:unknown" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct unknown-source URI: 'unknown:unknown'" );
        }

        try
        {
            POM_ROOT_URI = new URI( "pom:root" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct pom-root URI: 'pom:root'" );
        }
    }

}
