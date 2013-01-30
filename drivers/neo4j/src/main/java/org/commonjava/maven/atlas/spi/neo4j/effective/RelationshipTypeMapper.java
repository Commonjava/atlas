package org.commonjava.maven.atlas.spi.neo4j.effective;

import org.neo4j.graphdb.RelationshipType;

public enum RelationshipTypeMapper
    implements RelationshipType
{
    PARENT( org.apache.maven.graph.common.RelationshipType.PARENT ), DEPENDENCY(
        org.apache.maven.graph.common.RelationshipType.DEPENDENCY ), PLUGIN(
        org.apache.maven.graph.common.RelationshipType.PLUGIN ), PLUGIN_DEP(
        org.apache.maven.graph.common.RelationshipType.PLUGIN_DEP ), EXTENSION(
        org.apache.maven.graph.common.RelationshipType.EXTENSION );

    private org.apache.maven.graph.common.RelationshipType atlasType;

    private RelationshipTypeMapper( final org.apache.maven.graph.common.RelationshipType atlasType )
    {
        this.atlasType = atlasType;
    }

    public org.apache.maven.graph.common.RelationshipType atlasType()
    {
        return atlasType;
    }
}
