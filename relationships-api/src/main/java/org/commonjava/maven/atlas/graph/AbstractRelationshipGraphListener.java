package org.commonjava.maven.atlas.graph;

import java.util.Collection;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractRelationshipGraphListener
    implements RelationshipGraphListener
{

    protected AbstractRelationshipGraphListener()
    {
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals( Object other );

    @Override
    public void storing( final RelationshipGraph graph, final Collection<? extends ProjectRelationship<?>> relationships )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void stored( final RelationshipGraph graph,
                        final Collection<? extends ProjectRelationship<?>> relationships,
                        final Collection<ProjectRelationship<?>> rejected )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void projectError( final RelationshipGraph graph, final ProjectVersionRef ref,
                              final Throwable error )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void closing( final RelationshipGraph graph )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void closed( final RelationshipGraph graph )
        throws RelationshipGraphException
    {
        // NOP
    }

}
