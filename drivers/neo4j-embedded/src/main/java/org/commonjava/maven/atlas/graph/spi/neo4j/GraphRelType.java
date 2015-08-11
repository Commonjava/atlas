/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.RelationshipType;

public enum GraphRelType
    implements RelationshipType
{
    PARENT( org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT ),
    BOM( org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM ),
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

    GraphRelType()
    {
    }

    GraphRelType( final org.commonjava.maven.atlas.graph.rel.RelationshipType atlasType )
    {
        this.atlasType = atlasType;
    }

    GraphRelType( final org.commonjava.maven.atlas.graph.rel.RelationshipType atlasType, final boolean managed )
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

    public static GraphRelType map( final org.commonjava.maven.atlas.graph.rel.RelationshipType type,
                                    final boolean managed )
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

    public static GraphRelType[] atlasRelationshipTypes()
    {
        final Set<GraphRelType> types = new HashSet<GraphRelType>();
        for ( final GraphRelType type : values() )
        {
            if ( type.isAtlasRelationship() )
            {
                types.add( type );
            }
        }

        return types.toArray( new GraphRelType[types.size()] );
    }

    public static GraphRelType[] concreteAtlasRelationshipTypes()
    {
        final Set<GraphRelType> types = new HashSet<GraphRelType>();
        for ( final GraphRelType type : values() )
        {
            if ( type.isAtlasRelationship() && !type.managed )
            {
                types.add( type );
            }
        }

        return types.toArray( new GraphRelType[types.size()] );
    }

    public static GraphRelType[] managedAtlasRelationshipTypes()
    {
        final Set<GraphRelType> types = new HashSet<GraphRelType>();
        for ( final GraphRelType type : values() )
        {
            if ( type.isAtlasRelationship() && type.managed )
            {
                types.add( type );
            }
        }

        return types.toArray( new GraphRelType[types.size()] );
    }
}
