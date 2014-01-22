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
package org.commonjava.maven.atlas.ident;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum DependencyScope
{
    _import( "import" ), toolchain, provided, embedded, compile( provided, embedded ), runtime( compile ), system, test( provided, embedded, compile,
        runtime, system );

    private final Set<DependencyScope> implied;

    private String realName;

    private DependencyScope( final String realName, final DependencyScope... implied )
    {
        this.realName = realName;
        this.implied = new HashSet<DependencyScope>( Arrays.asList( implied ) );
    }

    private DependencyScope( final DependencyScope... implied )
    {
        realName = name();
        this.implied = new HashSet<DependencyScope>( Arrays.asList( implied ) );
    }

    public boolean implies( final DependencyScope scope )
    {
        return scope == this || implied.contains( scope );
    }

    public String realName()
    {
        return realName;
    }

    public static DependencyScope getScope( String scope )
    {
        if ( scope == null )
        {
            return null;
        }

        scope = scope.trim()
                     .toLowerCase();

        for ( final DependencyScope ds : values() )
        {
            if ( ds.realName.equals( scope ) )
            {
                return ds;
            }
        }

        return null;
    }

    public static DependencyScope[] parseScopes( final String scopesStr )
    {
        final String[] rawScopes = scopesStr.split( "\\s*[+,|]+\\s*" );
        final List<DependencyScope> result = new ArrayList<DependencyScope>( rawScopes.length );
        for ( final String rawScope : rawScopes )
        {
            if ( rawScope == null || rawScope.trim()
                                             .length() < 1 )
            {
                continue;
            }

            final DependencyScope scope = getScope( rawScope );
            if ( scope != null && !result.contains( scope ) )
            {
                result.add( scope );
            }
        }

        return result.toArray( new DependencyScope[result.size()] );
    }

}
