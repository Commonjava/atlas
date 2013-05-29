package org.commonjava.maven.atlas.spi.neo4j.effective;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.session.EGraphSession;
import org.apache.maven.graph.effective.session.EGraphSessionConfiguration;
import org.apache.maven.graph.spi.GraphDriverException;

public class NeoGraphSession
    extends EGraphSession
{

    private final long sessionId;

    NeoGraphSession( final long sessionNode, final AbstractNeo4JEGraphDriver driver,
                     final EGraphSessionConfiguration config )
    {
        super( Long.toString( sessionNode ), driver, config );
        this.sessionId = sessionNode;
    }

    public long getSessionId()
    {
        return sessionId;
    }

    @Override
    protected void selectionAdded( final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        ( (AbstractNeo4JEGraphDriver) getDriver() ).selectVersionFor( ref, version, sessionId );
    }

    @Override
    protected void sessionClosed()
    {
        ( (AbstractNeo4JEGraphDriver) getDriver() ).deleteSession( sessionId );
    }

    @Override
    protected void selectionsCleared()
    {
        ( (AbstractNeo4JEGraphDriver) getDriver() ).clearSelectedVersions( sessionId );
    }

}
