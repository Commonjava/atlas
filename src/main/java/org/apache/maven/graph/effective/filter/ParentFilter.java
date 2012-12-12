package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class ParentFilter
    implements ProjectRelationshipFilter
{

    public boolean accept( final ProjectRelationship<?> rel )
    {
        return rel instanceof ParentRelationship;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

}
