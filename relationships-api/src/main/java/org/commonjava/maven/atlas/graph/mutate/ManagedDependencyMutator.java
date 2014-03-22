package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedDependencyMutator
    extends VersionManagerMutator
    implements GraphMutator
{

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel, final GraphPath<?> path, final GraphView view )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY )
        {
            //            logger.debug( "No selections for relationships of type: {}", rel.getType() );
            return rel;
        }

        ProjectRelationship<?> mutated = super.selectFor( rel, path, view );
        if ( mutated == null || mutated == rel )
        {
            final ProjectVersionRef managed = view.getDatabase()
                                                  .getManagedTargetFor( rel.getTarget(), path, RelationshipType.DEPENDENCY );
            if ( managed != null )
            {
                mutated = rel.selectTarget( managed );
            }
        }

        if ( rel != mutated )
        {
            logger.debug( "Mutated. Was:\n  {}\n\nNow:\n  {}\n\n", rel, mutated );
        }

        return mutated == null ? rel : mutated;
    }
}
