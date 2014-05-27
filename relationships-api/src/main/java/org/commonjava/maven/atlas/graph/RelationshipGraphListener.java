package org.commonjava.maven.atlas.graph;

import java.util.Collection;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface RelationshipGraphListener
{

    void projectError( RelationshipGraph graph, ProjectVersionRef ref, Throwable error )
        throws RelationshipGraphException;

    void storing( RelationshipGraph graph, Collection<? extends ProjectRelationship<?>> relationships )
        throws RelationshipGraphException;

    void stored( RelationshipGraph graph, Collection<? extends ProjectRelationship<?>> relationships,
                 Collection<ProjectRelationship<?>> rejected )
        throws RelationshipGraphException;

    void closing( RelationshipGraph graph )
        throws RelationshipGraphException;

    void closed( RelationshipGraph graph )
        throws RelationshipGraphException;

}
