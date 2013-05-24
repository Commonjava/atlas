package org.apache.maven.graph.effective.session;


public final class SessionSelectionClearEvent
    extends EGraphSessionEvent
{

    public SessionSelectionClearEvent( final EGraphSession session )
    {
        super( session, EGraphSessionEventType.SELECTION_ADD );
    }

}
