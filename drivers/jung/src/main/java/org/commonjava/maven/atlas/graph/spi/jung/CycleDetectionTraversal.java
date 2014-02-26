package org.commonjava.maven.atlas.graph.spi.jung;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

final class CycleDetectionTraversal
    extends AbstractTraversal
{
    private final List<EProjectCycle> cycles = new ArrayList<EProjectCycle>();

    private final ProjectRelationship<?> rel;

    CycleDetectionTraversal( final ProjectRelationship<?> rel )
    {
        this.rel = rel;
    }

    public List<EProjectCycle> getCycles()
    {
        return cycles;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( rel.getDeclaring()
                .equals( rel.getTarget()
                            .asProjectVersionRef() ) )
        {
            return false;
        }

        new Logger( getClass() ).info( "Checking for cycle:\n\n%s\n\n", join( path, "\n" ) );

        final ProjectVersionRef from = rel.getDeclaring();
        if ( from.equals( relationship.getTarget()
                                      .asProjectVersionRef() ) )
        {
            final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>( path );
            cycle.add( rel );

            cycles.add( new EProjectCycle( cycle ) );
            return false;
        }

        return true;
    }
}