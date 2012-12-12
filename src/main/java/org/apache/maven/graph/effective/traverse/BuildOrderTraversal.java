package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class BuildOrderTraversal
    extends AbstractFilteringTraversal
{

    private final List<ProjectRef> order = new ArrayList<ProjectRef>();

    public BuildOrderTraversal()
    {
    }

    public BuildOrderTraversal( final ProjectRelationshipFilter filter )
    {
        super( filter );
    }

    public List<ProjectRef> getBuildOrder()
    {
        return order;
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
        final ProjectVersionRef decl = relationship.getDeclaring();

        ProjectVersionRef target = relationship.getTarget();
        if ( target instanceof ArtifactRef )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        final ProjectRef baseDecl = new ProjectRef( decl.getGroupId(), decl.getArtifactId() );
        final ProjectRef baseTgt = new ProjectRef( target.getGroupId(), target.getArtifactId() );

        int declIdx = order.indexOf( baseDecl );
        final int tgtIdx = order.indexOf( baseTgt );
        if ( declIdx < 0 )
        {
            declIdx = order.size();
            order.add( baseDecl );
        }

        if ( tgtIdx < 0 )
        {
            order.add( declIdx, baseTgt );
        }

        return true;
    }

}
