/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum DependencyScope
{
    _import( "import" ), provided, compile( provided ), runtime( compile ), system, test( provided, compile, runtime,
        system );

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

}
