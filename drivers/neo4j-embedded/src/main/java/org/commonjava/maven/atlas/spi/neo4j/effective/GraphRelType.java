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
package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.RelationshipType;

public enum GraphRelType
    implements RelationshipType
{
    PARENT( org.apache.maven.graph.common.RelationshipType.PARENT ), C_DEPENDENCY(
        org.apache.maven.graph.common.RelationshipType.DEPENDENCY ), C_PLUGIN(
        org.apache.maven.graph.common.RelationshipType.PLUGIN ), C_PLUGIN_DEP(
        org.apache.maven.graph.common.RelationshipType.PLUGIN_DEP ), M_DEPENDENCY(
        org.apache.maven.graph.common.RelationshipType.DEPENDENCY, true ), M_PLUGIN(
        org.apache.maven.graph.common.RelationshipType.PLUGIN, true ), M_PLUGIN_DEP(
        org.apache.maven.graph.common.RelationshipType.PLUGIN_DEP, true ), EXTENSION(
        org.apache.maven.graph.common.RelationshipType.EXTENSION ), CYCLE;

    private org.apache.maven.graph.common.RelationshipType atlasType;

    private boolean managed = false;

    private GraphRelType()
    {
    }

    private GraphRelType( final org.apache.maven.graph.common.RelationshipType atlasType )
    {
        this.atlasType = atlasType;
    }

    private GraphRelType( final org.apache.maven.graph.common.RelationshipType atlasType, final boolean managed )
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

    public org.apache.maven.graph.common.RelationshipType atlasType()
    {
        return atlasType;
    }

    public static GraphRelType map( final org.apache.maven.graph.common.RelationshipType type, final boolean managed )
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
