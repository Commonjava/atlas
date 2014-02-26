package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

final class PathDetectionTraversal
    extends AbstractTraversal
{
    //        private final Logger logger = new Logger( getClass() );

    private final ProjectVersionRef[] to;

    private final Set<List<ProjectRelationship<?>>> paths = new HashSet<List<ProjectRelationship<?>>>();

    PathDetectionTraversal( final ProjectVersionRef[] refs )
    {
        this.to = new ProjectVersionRef[refs.length];
        for ( int i = 0; i < refs.length; i++ )
        {
            this.to[i] = refs[i].asProjectVersionRef();
        }
    }

    public Set<List<ProjectRelationship<?>>> getPaths()
    {
        return paths;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        final ProjectVersionRef target = relationship.getTarget()
                                                     .asProjectVersionRef();

        // logger.info( "Checking path: %s to see if target: %s is in endpoints: %s", join( path, "," ), target, join( to, ", " ) );
        boolean found = false;
        for ( final ProjectVersionRef t : to )
        {
            if ( t.equals( target ) )
            {
                paths.add( new ArrayList<ProjectRelationship<?>>( path ) );
                // logger.info( "+= %s", join( path, ", " ) );
                found = true;
            }
        }

        return !found;
    }
}