package org.apache.maven.graph.effective.traverse.print;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public interface StructureRelationshipPrinter
{
    void print( ProjectRelationship<?> relationship, StringBuilder builder );
}