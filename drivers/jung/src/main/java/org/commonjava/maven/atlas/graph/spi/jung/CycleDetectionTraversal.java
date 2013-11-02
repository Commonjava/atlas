package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    private final Set<ProjectRelationship<?>> existingCycleParticipants;

    private final Set<ProjectRelationship<?>> rels;

    public CycleDetectionTraversal( final Collection<EProjectCycle> existingCycles, final Set<ProjectRelationship<?>> skipped,
                                    final ProjectRelationship<?>... rels )
    {
        this.rels = new HashSet<>( Arrays.asList( rels ) );
        this.rels.removeAll( skipped );

        existingCycleParticipants = new HashSet<>();
        for ( final EProjectCycle cycle : existingCycles )
        {
            existingCycleParticipants.addAll( cycle.getAllRelationships() );
        }
    }

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
        if ( existingCycleParticipants.contains( relationship ) || hits.contains( relationship ) )
        {
            logger.info( "%s is part of an existing cycle. Skip traversal.", relationship );
            found = true;
        }

        if ( !found && relationship.getDeclaring()
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

        if ( !found )
        {
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
        }

        if ( hits.containsAll( rels ) )
        {
            logger.info( "Accounted for all new relationships. Stopping analysis." );
            stop();
        }

        return !found;
    }
}