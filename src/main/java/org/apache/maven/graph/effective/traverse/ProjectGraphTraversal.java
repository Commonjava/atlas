package org.apache.maven.graph.effective.traverse;

import java.util.List;

import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public interface ProjectGraphTraversal
{

    TraversalType getType( int pass );

    int getRequiredPasses();

    void startTraverse( int pass, EProjectGraph graph );

    void endTraverse( int pass, EProjectGraph graph );

    boolean traverseEdge( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

}
