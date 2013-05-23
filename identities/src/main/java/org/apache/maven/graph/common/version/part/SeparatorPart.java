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

import java.io.Serializable;

public class SeparatorPart
    extends VersionPart
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final VersionPartSeparator type;

    public SeparatorPart( final VersionPartSeparator type )
    {
        this.type = type;
    }

    @Override
    public String renderStandard()
    {
        return type.getRenderedString();
    }

    public VersionPartSeparator getValue()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return String.format( "SEP[%s]", type.getRenderedString() );
    }

    public int compareTo( final VersionPart o )
    {
        return 0;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final SeparatorPart other = (SeparatorPart) obj;
        if ( type != other.type )
        {
            return false;
        }
        return true;
    }

}
