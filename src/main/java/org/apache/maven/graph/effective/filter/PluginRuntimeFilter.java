package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class PluginRuntimeFilter
    implements ProjectRelationshipFilter
{

    public PluginRuntimeFilter()
    {
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        return ( rel instanceof PluginRelationship ) && !( (PluginRelationship) rel ).isManaged();
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        final PluginRelationship plugin = (PluginRelationship) parent;

        final OrFilter child =
            new OrFilter( new DependencyFilter( DependencyScope.runtime ), new PluginDependencyFilter( plugin ) );
        return child;
    }

}
