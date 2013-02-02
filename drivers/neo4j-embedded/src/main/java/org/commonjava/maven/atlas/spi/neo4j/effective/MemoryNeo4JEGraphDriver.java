package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.Arrays;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.impl.MemoryGraphDatabaseFactory;

public class MemoryNeo4JEGraphDriver
    extends AbstractNeo4JEGraphDriver
{

    //    private final Logger logger = new Logger( getClass() );

    public MemoryNeo4JEGraphDriver()
    {
        this( true );
    }

    public MemoryNeo4JEGraphDriver( final boolean useShutdownHook )
    {
        super( new MemoryGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                                               .newGraphDatabase(), useShutdownHook );
    }

    private MemoryNeo4JEGraphDriver( final MemoryNeo4JEGraphDriver driver )
    {
        super( driver );
    }

    public EGraphDriver newInstance()
        throws GraphDriverException
    {
        return new MemoryNeo4JEGraphDriver( this );
    }

    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectVersionRef... refs )
    {
        final MemoryNeo4JEGraphDriver driver = new MemoryNeo4JEGraphDriver( this );
        driver.restrictToRoots( Arrays.asList( refs ), net );

        return driver;
    }

}
