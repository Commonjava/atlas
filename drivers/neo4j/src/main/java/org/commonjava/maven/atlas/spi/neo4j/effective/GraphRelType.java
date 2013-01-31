package org.commonjava.maven.atlas.spi.neo4j.effective;

import org.neo4j.graphdb.RelationshipType;

public enum GraphRelType
    implements RelationshipType
{
    PARENT( org.apache.maven.graph.common.RelationshipType.PARENT ), DEPENDENCY(
        org.apache.maven.graph.common.RelationshipType.DEPENDENCY ), PLUGIN(
        org.apache.maven.graph.common.RelationshipType.PLUGIN ), PLUGIN_DEP(
        org.apache.maven.graph.common.RelationshipType.PLUGIN_DEP ), EXTENSION(
        org.apache.maven.graph.common.RelationshipType.EXTENSION );

    private org.apache.maven.graph.common.RelationshipType atlasType;

    private GraphRelType()
    {
    }

    private GraphRelType( final org.apache.maven.graph.common.RelationshipType atlasType )
    {
        this.atlasType = atlasType;
    }

    public boolean isAtlasRelationship()
    {
        return atlasType != null;
    }

    public org.apache.maven.graph.common.RelationshipType atlasType()
    {
        return atlasType;
    }

    public static RelationshipType map( final org.apache.maven.graph.common.RelationshipType type )
    {
        for ( final GraphRelType mapper : values() )
        {
            if ( mapper.atlasType == type )
            {
                return mapper;
            }
        }

        return null;
    }
}
