package org.apache.maven.graph.effective;

import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

import edu.uci.ics.jung.graph.DirectedGraph;

public interface EProjectNet
    extends EProjectRelationshipCollection
{

    DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> getRawGraph();

    boolean isComplete();

    boolean isConcrete();

    Set<ProjectVersionRef> getIncompleteSubgraphs();

    Set<ProjectVersionRef> getVariableSubgraphs();

    void recomputeIncompleteSubgraphs();

    Set<EProjectCycle> getCycles();

    void addCycle( final EProjectCycle cycle );

    boolean isCycleParticipant( final ProjectRelationship<?> rel );

    boolean isCycleParticipant( final ProjectVersionRef ref );

    public abstract Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref );

}