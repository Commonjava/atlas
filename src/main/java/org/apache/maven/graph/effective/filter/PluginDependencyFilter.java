package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

// TODO: Do we need to consider excludes in the direct plugin-level dependency?
public class PluginDependencyFilter
    implements ProjectRelationshipFilter
{

    private final ProjectRef plugin;

    public PluginDependencyFilter( final PluginRelationship plugin )
    {
        this.plugin = plugin.getTarget()
                            .asProjectRef();
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        if ( rel instanceof PluginDependencyRelationship )
        {
            final PluginDependencyRelationship pdr = (PluginDependencyRelationship) rel;
            return plugin.equals( pdr.getPlugin() );
        }

        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new DependencyFilter( DependencyScope.runtime );
    }

}
