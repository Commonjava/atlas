/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.rel;

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
