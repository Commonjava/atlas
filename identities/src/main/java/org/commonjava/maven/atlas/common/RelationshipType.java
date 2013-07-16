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
package org.commonjava.maven.atlas.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum RelationshipType
{

    PARENT( "parent" ), DEPENDENCY( "dependency", "dep" ), PLUGIN( "plugin" ), PLUGIN_DEP( "plugin-dependency",
        "plugin-dep", "plugin-level-dependency", "plugin-level-dep" ), EXTENSION( "extension", "ext" );

    private final Set<String> names;

    private RelationshipType( final String... names )
    {
        this.names = Collections.unmodifiableSet( new HashSet<String>( Arrays.asList( names ) ) );
    }

    public Set<String> names()
    {
        return names;
    }

    public static RelationshipType getType( String type )
    {
        if ( type == null || type.trim()
                                 .length() < 1 )
        {
            return null;
        }

        type = type.trim();
        for ( final RelationshipType rt : values() )
        {
            for ( final String name : rt.names() )
            {
                if ( name.equalsIgnoreCase( type ) )
                {
                    return rt;
                }
            }
        }

        return null;
    }

}
