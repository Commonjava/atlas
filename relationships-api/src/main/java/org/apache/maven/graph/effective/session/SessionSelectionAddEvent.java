package org.apache.maven.graph.effective.session;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;

public final class SessionSelectionAddEvent
    extends EGraphSessionEvent
{

    private final ProjectVersionRef ref;

    private final SingleVersion selected;

    public SessionSelectionAddEvent( final ProjectVersionRef ref, final SingleVersion selected,
                                     final EGraphSession session )
    {
        super( session, EGraphSessionEventType.SELECTION_ADD );
        this.ref = ref;
        this.selected = selected;
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    public SingleVersion getSelected()
    {
        return selected;
    }

}
