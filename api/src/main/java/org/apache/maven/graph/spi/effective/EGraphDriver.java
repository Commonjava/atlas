package org.apache.maven.graph.spi.effective;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;

public interface EGraphDriver
    extends Closeable
{

    EGraphDriver newInstance()
        throws GraphDriverException;

    void restrictProjectMembership( Collection<ProjectVersionRef> refs );

    void restrictRelationshipMembership( Collection<ProjectRelationship<?>> rels );

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships();

    boolean addRelationship( ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects();

    void traverse( ProjectNetTraversal traversal, EProjectNet net, ProjectVersionRef root )
        throws GraphDriverException;

    boolean containsProject( ProjectVersionRef ref );

    boolean containsRelationship( ProjectRelationship<?> rel );

    boolean isDerivedFrom( EGraphDriver driver );

    boolean isMissing( ProjectVersionRef project );

    boolean hasMissingProjects();

    Set<ProjectVersionRef> getMissingProjects();

    boolean hasVariableProjects();

    Set<ProjectVersionRef> getVariableProjects();

    boolean addCycle( EProjectCycle cycle );

    Set<EProjectCycle> getCycles();

    boolean isCycleParticipant( ProjectRelationship<?> rel );

    boolean isCycleParticipant( ProjectVersionRef ref );

    void recomputeIncompleteSubgraphs();

    Map<String, String> getProjectMetadata( ProjectVersionRef ref );

    void addProjectMetadata( ProjectVersionRef ref, String key, String value );

    void addProjectMetadata( ProjectVersionRef ref, Map<String, String> metadata );

    EGraphDriver newInstanceFrom( EProjectNet net, ProjectVersionRef... refs );

}
