package org.commonjava.maven.atlas.spi.neo4j.effective;

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
}
