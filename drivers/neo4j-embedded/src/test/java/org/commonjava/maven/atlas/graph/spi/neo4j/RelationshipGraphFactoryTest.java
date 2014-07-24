package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.fixture.FileConnectionFixture;
import org.commonjava.maven.atlas.tck.graph.RelationshipGraphFactoryTCK;
import org.junit.Rule;

public class RelationshipGraphFactoryTest
    extends RelationshipGraphFactoryTCK
{
    @Rule
    public FileConnectionFixture fixture = new FileConnectionFixture();

    @Override
    protected synchronized RelationshipGraphConnectionFactory connectionFactory()
        throws Exception
    {
        return fixture.connectionFactory();
    }
}
