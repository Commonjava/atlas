package org.apache.maven.graph.effective.session;

public abstract class EGraphSessionEvent
{

    private final EGraphSession session;

    private final EGraphSessionEventType type;

    protected EGraphSessionEvent( final EGraphSession session, final EGraphSessionEventType type )
    {
        this.session = session;
        this.type = type;
    }

    public final EGraphSessionEventType getType()
    {
        return type;
    }

    public final EGraphSession getSession()
    {
        return session;
    }

}
