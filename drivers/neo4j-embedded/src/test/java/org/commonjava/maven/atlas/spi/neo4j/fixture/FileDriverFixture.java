package org.commonjava.maven.atlas.spi.neo4j.fixture;

import java.io.File;

import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.effective.FileNeo4JEGraphDriver;
import org.junit.rules.TemporaryFolder;

public class FileDriverFixture
    extends AbstractDriverFixture
{

    private final TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected EGraphDriver create()
        throws Exception
    {
        final File dbDir = folder.newFolder();
        dbDir.delete();
        dbDir.mkdirs();

        return new FileNeo4JEGraphDriver( dbDir );
    }

    @Override
    protected void after()
    {
        super.after();
        folder.delete();
    }

    @Override
    protected void before()
        throws Throwable
    {
        super.before();
        folder.create();
    }
}
