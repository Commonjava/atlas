package org.apache.maven.graph.effective.traverse;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public abstract class AbstractTraversal
    implements ProjectNetTraversal
{

    private List<TraversalType> types;

    protected AbstractTraversal()
    {
    }

    protected AbstractTraversal( final TraversalType... types )
    {
        this.types = Arrays.asList( types );
    }

    public TraversalType getType( final int pass )
    {
        if ( types == null || types.isEmpty() )
        {
            return TraversalType.depth_first;
        }
        else if ( pass >= types.size() )
        {
            return types.get( types.size() - 1 );
        }

        return types.get( pass );
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

    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                               final int pass )
    {
    }

    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        return true;
    }
}
