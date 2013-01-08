package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class AnyFilter
    implements ProjectRelationshipFilter
{

    public boolean accept( final ProjectRelationship<?> rel )
    {
        return true;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

}
