package org.apache.maven.graph.spi.effective;

import java.io.Closeable;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;

public interface EGraphDriver
    extends Closeable
{

    EGraphDriver newInstance()
        throws GraphDriverException;

    void restrictProjectMembership( Set<ProjectVersionRef> refs );

    void restrictRelationshipMembership( Set<ProjectRelationship<?>> rels );

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships();

    boolean addRelationship( ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects();

    void traverse( ProjectNetTraversal traversal, EProjectNet net, ProjectVersionRef root )
        throws GraphDriverException;

    boolean containsProject( ProjectVersionRef ref );

    boolean containsRelationship( ProjectRelationship<?> rel );

    void selectVersion( ProjectVersionRef ref, VersionSpec spec );

    Set<ProjectVersionRef> getUnconnectedProjectReferences();

    Set<ProjectVersionRef> getVariableProjectReferences();

}
