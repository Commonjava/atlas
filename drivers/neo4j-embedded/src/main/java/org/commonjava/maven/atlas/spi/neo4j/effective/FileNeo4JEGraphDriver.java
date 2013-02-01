package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.io.File;

import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class FileNeo4JEGraphDriver
    extends AbstractNeo4JEGraphDriver
{

    //    private final Logger logger = new Logger( getClass() );

    public FileNeo4JEGraphDriver( final File dbPath )
    {
        this( dbPath, true );
    }

    public FileNeo4JEGraphDriver( final File dbPath, final boolean useShutdownHook )
    {
        super( new GraphDatabaseFactory().newEmbeddedDatabase( dbPath.getAbsolutePath() ), useShutdownHook );
    }

    private FileNeo4JEGraphDriver( final FileNeo4JEGraphDriver driver )
    {
        super( driver );
    }

    public EGraphDriver newInstance()
        throws GraphDriverException
    {
        return new FileNeo4JEGraphDriver( this );
    }

}
