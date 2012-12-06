package org.apache.maven.graph.effective.traverse;

import java.util.List;

import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public interface ProjectNetTraversal
{

    TraversalType getType( int pass );

    int getRequiredPasses();

    void startTraverse( int pass, EProjectNet network );

    void endTraverse( int pass, EProjectNet network );

    boolean traverseEdge( ProjectRelationship<?> relationship, List<ProjectRelationship<?>> path, int pass );

}
