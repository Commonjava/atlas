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

import static org.commonjava.maven.atlas.ident.DependencyScope.embedded;
import static org.commonjava.maven.atlas.ident.DependencyScope.runtime;
import static org.commonjava.maven.atlas.ident.DependencyScope.toolchain;

public enum ScopeTransitivity
{
    maven
    {
        @Override
        public DependencyScope getChildFor( final DependencyScope scope )
        {
            switch ( scope )
            {
                case embedded:
                    return embedded;
                case toolchain:
                    return toolchain;
                default:
                    return runtime;
            }
        }
    },

    all
    {
        @Override
        public DependencyScope getChildFor( final DependencyScope scope )
        {
            return scope;
        }
    };

    public abstract DependencyScope getChildFor( DependencyScope scope );

}
