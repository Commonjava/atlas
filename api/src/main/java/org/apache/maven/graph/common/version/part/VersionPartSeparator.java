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
package org.apache.maven.graph.common.version.part;

public enum VersionPartSeparator
{

    BLANK( "" ), DASH( "-" ), UNDERSCORE( "_" ), DOT( "." );

    private String rendered;

    private VersionPartSeparator( final String rendered )
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
