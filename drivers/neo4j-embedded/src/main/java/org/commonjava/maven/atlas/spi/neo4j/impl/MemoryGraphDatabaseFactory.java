package org.commonjava.maven.atlas.spi.neo4j.impl;

import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class MemoryGraphDatabaseFactory
    extends GraphDatabaseFactory
{
    public GraphDatabaseService newImpermanentDatabase()
    {
        return newImpermanentDatabaseBuilder().newGraphDatabase();
    }

    public GraphDatabaseBuilder newImpermanentDatabaseBuilder()
    {
        return new GraphDatabaseBuilder( new GraphDatabaseBuilder.DatabaseCreator()
        {
            public GraphDatabaseService newDatabase( final Map<String, String> config )
            {
                config.put( "ephemeral", "true" );
                return new ImpermanentGraphDatabase( config, indexProviders, kernelExtensions, cacheProviders );
            }
        } );
    }
}
