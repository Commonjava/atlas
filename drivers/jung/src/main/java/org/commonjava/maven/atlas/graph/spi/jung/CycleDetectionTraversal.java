package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

final class CycleDetectionTraversal
    extends AbstractTraversal
{
    private final Logger logger = new Logger( getClass() );

    private final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final Set<ProjectRelationship<?>> hits = new HashSet<>();

    public Set<ProjectRelationship<?>> getCycleInjectors()
    {
        return hits;
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        boolean found = false;
        int i = 0;
        for ( final ProjectRelationship<?> rel : path )
        {
            final ProjectVersionRef from = rel.getDeclaring();

            if ( from.equals( relationship.getTarget()
                                          .asProjectVersionRef() ) )
            {
                final List<ProjectRelationship<?>> sub = path.subList( i, path.size() );
                final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>( sub );
                cycle.add( relationship );

                logger.info( "Found cycle: %s", cycle );

                cycles.add( new EProjectCycle( cycle ) );
                hits.add( relationship );

                found = true;
            }

            i++;
        }

        if ( relationship.getDeclaring()
                         .equals( relationship.getTarget()
                                              .asProjectVersionRef() ) )
        {
            final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>();
            cycle.add( relationship );

            logger.info( "Found cycle: %s", cycle );

            cycles.add( new EProjectCycle( cycle ) );
            hits.add( relationship );

            found = true;
        }

        return !found;
    }
}