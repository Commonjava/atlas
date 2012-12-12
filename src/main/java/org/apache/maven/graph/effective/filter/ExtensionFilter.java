package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

// TODO: Do we need to consider excludes in the direct plugin-level dependency?
public class ExtensionFilter
    implements ProjectRelationshipFilter
{

    public boolean accept( final ProjectRelationship<?> rel )
    {
        return rel instanceof ExtensionRelationship;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new DependencyFilter( DependencyScope.runtime );
    }

}
