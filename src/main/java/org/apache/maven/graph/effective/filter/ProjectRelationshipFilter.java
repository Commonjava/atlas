package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public interface ProjectRelationshipFilter
{

    boolean accept( ProjectRelationship<?> rel );

    ProjectRelationshipFilter getChildFilter( ProjectRelationship<?> parent );

}
