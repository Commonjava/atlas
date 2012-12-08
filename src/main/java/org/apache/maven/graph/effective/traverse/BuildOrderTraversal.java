package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class BuildOrderTraversal
    implements ProjectNetTraversal
{

    private final List<ProjectRef> order = new ArrayList<ProjectRef>();

    private final RelationshipType[] types;

    public BuildOrderTraversal( final RelationshipType... types )
    {
        this.types = types;
        Arrays.sort( types );
    }

    public List<ProjectRef> getBuildOrder()
    {
        return order;
    }

    public TraversalType getType( final int pass )
    {
        return TraversalType.depth_first;
    }

    public int getRequiredPasses()
    {
        return 1;
    }

    public void startTraverse( final int pass, final EProjectNet network )
    {
    }

    public void endTraverse( final int pass, final EProjectNet network )
    {
    }

    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        if ( relationship instanceof DependencyRelationship && ( (DependencyRelationship) relationship ).isManaged() )
        {
            return false;
        }

        if ( relationship instanceof PluginRelationship && ( (PluginRelationship) relationship ).isManaged() )
        {
            return false;
        }

        if ( types != null && types.length > 0 && Arrays.binarySearch( types, relationship.getType() ) < 0 )
        {
            return false;
        }

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
