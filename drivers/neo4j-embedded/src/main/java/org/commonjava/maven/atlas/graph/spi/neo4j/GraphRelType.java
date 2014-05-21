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
