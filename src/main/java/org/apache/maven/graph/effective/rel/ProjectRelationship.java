package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.VersionedProjectRef;

public interface ProjectRelationship<T extends VersionedProjectRef>
{

    int getIndex();

    RelationshipType getType();

    VersionedProjectRef getDeclaring();

    T getTarget();

    ProjectRelationship<T> cloneFor( final VersionedProjectRef projectRef );

}
