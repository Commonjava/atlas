package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.graph.common.ref.VersionedProjectRef;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class AncestryTraversal
    implements ProjectGraphTraversal
{

    private final List<VersionedProjectRef> ancestry = new ArrayList<VersionedProjectRef>();

    public AncestryTraversal( final VersionedProjectRef startingFrom )
    {
        ancestry.add( startingFrom );
    }

    public AncestryTraversal()
    {
    }

    public List<VersionedProjectRef> getAncestry()
    {
        return Collections.unmodifiableList( ancestry );
    }

    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        if ( ancestry.get( ancestry.size() - 1 )
                     .equals( relationship.getDeclaring() ) && ( relationship instanceof ParentRelationship ) )
        {
            ancestry.add( relationship.getTarget() );
            return true;
        }

        return false;
    }

    public boolean isInAncestry( final VersionedProjectRef ref )
    {
        return ancestry.contains( ref );
    }

    public TraversalType getType( final int pass )
    {
        return TraversalType.depth_first;
    }

    public void startTraverse( final int pass, final EProjectGraph graph )
    {
        if ( ancestry.isEmpty() )
        {
            ancestry.add( graph.getRoot() );
        }
    }

    public int getRequiredPasses()
    {
        return 1;
    }

    public void endTraverse( final int pass, final EProjectGraph graph )
    {
    }

}
