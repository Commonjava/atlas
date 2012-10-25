package org.apache.maven.graph.common.version.part;

import java.io.Serializable;

public abstract class VersionPart
    implements Comparable<VersionPart>, Serializable
{

    private static final long serialVersionUID = 1L;

    private boolean silent = false;

    public abstract String renderStandard();

    final boolean isSilent()
    {
        return silent;
    }

    final void setSilent( final boolean silent )
    {
        this.silent = silent;
    }

}
