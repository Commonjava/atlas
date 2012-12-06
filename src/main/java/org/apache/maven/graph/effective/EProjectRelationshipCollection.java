package org.apache.maven.graph.effective;

import java.io.Serializable;
import java.util.Set;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public interface EProjectRelationshipCollection
    extends Serializable
{

    @SuppressWarnings( "rawtypes" )
    Set<ProjectRelationship> getAllRelationships();

    @SuppressWarnings( "rawtypes" )
    Set<ProjectRelationship> getExactAllRelationships();

}
