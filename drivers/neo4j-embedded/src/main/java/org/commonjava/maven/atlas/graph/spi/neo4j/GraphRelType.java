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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.RelationshipType;

public enum GraphRelType
    implements RelationshipType
{
    PARENT( org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT ),
    C_DEPENDENCY( org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY ),
    C_PLUGIN( org.commonjava.maven.atlas.graph.rel.RelationshipType.PLUGIN ),
    C_PLUGIN_DEP( org.commonjava.maven.atlas.graph.rel.RelationshipType.PLUGIN_DEP ),
    M_DEPENDENCY( org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY, true ),
    M_PLUGIN( org.commonjava.maven.atlas.graph.rel.RelationshipType.PLUGIN, true ),
    M_PLUGIN_DEP( org.commonjava.maven.atlas.graph.rel.RelationshipType.PLUGIN_DEP, true ),
    EXTENSION( org.commonjava.maven.atlas.graph.rel.RelationshipType.EXTENSION ),
    CYCLE,
    CACHED_PATH_RELATIONSHIP,
    CACHED_CYCLE_RELATIONSHIP;

    private org.commonjava.maven.atlas.graph.rel.RelationshipType atlasType;

    private boolean managed = false;

    private GraphRelType()
    {
    }

    private GraphRelType( final org.commonjava.maven.atlas.graph.rel.RelationshipType atlasType )
    {
        this.atlasType = atlasType;
    }

    private GraphRelType( final org.commonjava.maven.atlas.graph.rel.RelationshipType atlasType, final boolean managed )
    {
        this.atlasType = atlasType;
        this.managed = managed;
    }

    public boolean isManaged()
    {
        return managed;
    }

    public boolean isAtlasRelationship()
    {
        return atlasType != null;
    }

    public org.commonjava.maven.atlas.graph.rel.RelationshipType atlasType()
    {
        return atlasType;
    }

    public static GraphRelType map( final org.commonjava.maven.atlas.graph.rel.RelationshipType type, final boolean managed )
    {
        for ( final GraphRelType mapper : values() )
        {
            if ( mapper.atlasType == type && managed == mapper.managed )
            {
                return mapper;
            }
        }

        return null;
    }

    public static Set<GraphRelType> atlasRelationshipTypes()
    {
        final Set<GraphRelType> types = new HashSet<GraphRelType>();
        for ( final GraphRelType type : values() )
        {
            if ( type.isAtlasRelationship() )
            {
                types.add( type );
            }
        }

        return types;
    }
}
