package org.apache.maven.graph.effective.traverse.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.effective.EProjectCycle;

public final class BuildOrder
{

    private final List<ProjectRef> order;

    private final Set<EProjectCycle> cycles;

    public BuildOrder( final List<ProjectRef> order, final Set<EProjectCycle> cycles )
    {
        this.order = Collections.unmodifiableList( order );
        this.cycles = cycles == null ? null : Collections.unmodifiableSet( cycles );
    }

    public List<ProjectRef> getOrder()
    {
        return order;
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

}
