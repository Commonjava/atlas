package org.commonjava.maven.atlas.graph.traverse.print;

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface StructureRelationshipPrinter
{
    void print( ProjectRelationship<?> relationship, StringBuilder builder, Map<String, Set<ProjectVersionRef>> labels );
}