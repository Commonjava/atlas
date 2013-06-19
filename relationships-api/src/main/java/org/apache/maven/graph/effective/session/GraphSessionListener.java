package org.apache.maven.graph.effective.session;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.spi.GraphDriverException;

public interface GraphSessionListener
{

    void selectionAdded( EGraphSession session, ProjectVersionRef ref, SingleVersion version )
        throws GraphDriverException;

    void sessionClosed( EGraphSession session )
        throws GraphDriverException;

    void selectionsCleared( EGraphSession session )
        throws GraphDriverException;
}
