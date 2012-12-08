package org.apache.maven.graph.effective;

import java.io.Serializable;
import java.util.Collection;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public interface EProjectRelationshipCollection
    extends Serializable
{

    Collection<ProjectRelationship<?>> getAllRelationships();

    Collection<ProjectRelationship<?>> getExactAllRelationships();

}
