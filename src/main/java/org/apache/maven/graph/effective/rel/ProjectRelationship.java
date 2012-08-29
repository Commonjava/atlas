package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public interface ProjectRelationship<T extends ProjectVersionRef>
{

    int getIndex();

    RelationshipType getType();

    ProjectVersionRef getDeclaring();

    T getTarget();

    ProjectRelationship<T> cloneFor( final ProjectVersionRef projectRef );

}
