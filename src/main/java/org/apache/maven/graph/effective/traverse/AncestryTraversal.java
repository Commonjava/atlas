package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class AncestryTraversal
    implements ProjectNetTraversal
{

    private final List<ProjectVersionRef> ancestry = new ArrayList<ProjectVersionRef>();

    public AncestryTraversal( final ProjectVersionRef startingFrom )
    {
        ancestry.add( startingFrom );
    }

    public AncestryTraversal()
    {
    }

    public List<ProjectVersionRef> getAncestry()
    {
        return Collections.unmodifiableList( ancestry );
    }

    public boolean traverseEdge( final ProjectRelationship relationship, final List<ProjectRelationship> path,
                                 final int pass )
    {
        if ( relationship instanceof ParentRelationship )
        {
            if ( relationship.getDeclaring()
                             .equals( relationship.getTarget() ) )
            {
                // reached the end of the line. STOP.
                return false;
            }

            if ( ancestry.isEmpty() )
            {
                ancestry.add( relationship.getDeclaring() );
                ancestry.add( relationship.getTarget() );
                return true;
            }
            else if ( ancestry.get( ancestry.size() - 1 )
                              .equals( relationship.getDeclaring() ) )
            {
                ancestry.add( relationship.getTarget() );
                return true;
            }
        }

        return false;
    }

    public boolean isInAncestry( final ProjectVersionRef ref )
    {
        return ancestry.contains( ref );
    }

    public TraversalType getType( final int pass )
    {
        return TraversalType.depth_first;
    }

    public void startTraverse( final int pass, final EProjectNet network )
    {
    }

    public int getRequiredPasses()
    {
        return 1;
    }

    public void endTraverse( final int pass, final EProjectNet network )
    {
    }

}
