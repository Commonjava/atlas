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
package org.commonjava.atlas.maven.ident.version.part;

public enum VersionPartSeparator
{

    BLANK( "" ), DASH( "-" ), UNDERSCORE( "_" ), DOT( "." ), PLUS("+");

    private String rendered;

    VersionPartSeparator( final String rendered )
    {
        this.rendered = rendered;
    }

    public String getRenderedString()
    {
        return rendered;
    }

    public static VersionPartSeparator find( final String literal )
    {
        for ( final VersionPartSeparator vps : values() )
        {
            if ( vps.rendered.equals( literal ) )
            {
                return vps;
            }
        }

        return null;
    }

}
