package org.apache.maven.graph.common.version.part;

public abstract class VersionPart
    implements Comparable<VersionPart>
{

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
