package org.commonjava.maven.atlas.effective.traverse.print;

import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

public interface StructureRelationshipPrinter
{
    void print( ProjectRelationship<?> relationship, StringBuilder builder );
}