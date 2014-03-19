package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class NoOpGraphMutator
    implements GraphMutator
{

    private static final long serialVersionUID = 1L;

    public static final NoOpGraphMutator INSTANCE = new NoOpGraphMutator();

    private NoOpGraphMutator()
    {
    }

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel, final GraphPath<?> path, final GraphView view )
    {
        return rel;
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel, final GraphView view )
    {
        return this;
    }

    @Override
    public String getLongId()
    {
        return "NOP";
    }

    @Override
    public String getCondensedId()
    {
        return getLongId();
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public int hashCode()
    {
        return NoOpGraphMutator.class.hashCode() + 1;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        return true;
    }

}
