package org.apache.maven.graph.effective.session;

public class SessionCloseEvent
    extends EGraphSessionEvent
{

    public SessionCloseEvent( final EGraphSession session )
    {
        super( session, EGraphSessionEventType.CLOSE );
    }

}
