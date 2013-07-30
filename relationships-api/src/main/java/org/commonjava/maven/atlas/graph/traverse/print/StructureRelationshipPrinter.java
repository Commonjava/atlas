package org.commonjava.maven.atlas.graph.traverse.print;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public interface StructureRelationshipPrinter
{
    void print( ProjectRelationship<?> relationship, StringBuilder builder );
}