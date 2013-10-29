package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

final class SingleCycleDetectionTraversal
    extends AbstractTraversal
{
    private final Logger logger = new Logger( getClass() );

    private final List<EProjectCycle> cycles = new ArrayList<EProjectCycle>();

    private final ProjectRelationship<?> rel;

    public SingleCycleDetectionTraversal( final ProjectRelationship<?> rel )
    {
        this.rel = rel;
        if ( rel.getDeclaring()
                .equals( rel.getTarget()
                            .asProjectVersionRef() ) )
        {
            final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>();
            cycle.add( rel );

            logger.info( "Found cycle: %s", cycle );

            cycles.add( new EProjectCycle( cycle ) );
            stop();
        }
    }

    public List<EProjectCycle> getCycles()
    {
        return cycles;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        final ProjectRelationship<?> rootRel;
        if ( path.size() > 0 )
        {
            rootRel = path.get( 0 );
        }
        else
        {
            rootRel = relationship;
        }

        final ProjectVersionRef root = rootRel.getDeclaring();
        final ProjectVersionRef lastTarget = relationship.getTarget()
                                                         .asProjectVersionRef();

        final ProjectVersionRef relDecl = rel.getDeclaring();

        final ProjectVersionRef relTarget = rel.getTarget()
                                               .asProjectVersionRef();

        if ( relDecl.equals( lastTarget ) && root.equals( relTarget ) )
        {
            final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>( path );
            cycle.add( relationship );
            cycle.add( rel );

            logger.info( "Found cycle: %s", cycle );

            cycles.add( new EProjectCycle( cycle ) );
            stop();

            return false;
        }

        return true;
    }
}