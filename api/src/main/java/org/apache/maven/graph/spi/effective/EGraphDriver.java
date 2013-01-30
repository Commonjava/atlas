package org.apache.maven.graph.spi.effective;

import java.util.Collection;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;

public interface EGraphDriver
{

    EGraphDriver newInstance()
        throws GraphDriverException;

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships()
        throws GraphDriverException;

    boolean addRelationship( ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects();

    void traverse( ProjectNetTraversal traversal, EProjectNet net, ProjectVersionRef root )
        throws GraphDriverException;

    boolean containsProject( ProjectVersionRef ref );

    boolean containsRelationship( ProjectRelationship<?> rel );

}
