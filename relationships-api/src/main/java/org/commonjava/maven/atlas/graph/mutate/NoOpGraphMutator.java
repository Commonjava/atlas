package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.model.GraphPath;

public class NoOpGraphMutator
    implements GraphMutator
{

    public static final NoOpGraphMutator INSTANCE = new NoOpGraphMutator();

    private NoOpGraphMutator()
    {
    }

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel, final GraphPath<?> path )
    {
        return rel;
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel )
    {
        return this;
    }

}
