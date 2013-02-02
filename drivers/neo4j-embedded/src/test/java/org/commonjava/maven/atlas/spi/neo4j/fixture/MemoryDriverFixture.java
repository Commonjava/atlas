package org.commonjava.maven.atlas.spi.neo4j.fixture;

import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.effective.MemoryNeo4JEGraphDriver;

public class MemoryDriverFixture
    extends AbstractDriverFixture
{

    @Override
    protected EGraphDriver create()
    {
        return new MemoryNeo4JEGraphDriver( false );
    }

}
