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
package org.commonjava.atlas.maven.graph.rel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum RelationshipType
{

    PARENT( "parent" ), BOM( "bom" ), DEPENDENCY( "dependency", "dep" ), PLUGIN( "plugin" ), PLUGIN_DEP(
        "plugin-dependency", "plugin-dep", "plugin-level-dependency", "plugin-level-dep" ), EXTENSION( "extension",
        "ext" );

    private final Set<String> names;

    RelationshipType( final String... names )
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
            if ( rt.name().equals(type))
            {
                return rt;
            }

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
