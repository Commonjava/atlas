package org.apache.maven.graph.raw;

import java.util.List;

import org.apache.maven.graph.raw.ref.ProtoCoordinate;
import org.apache.maven.graph.raw.ref.ProtoDependency;
import org.apache.maven.graph.raw.ref.ProtoPluginDependency;

public class RProjectRelationships
{

    private final List<ProtoCoordinate> ancestry;

    private final List<ProtoDependency> dependencies;

    private final List<ProtoDependency> managedDependencies;

    private final List<ProtoCoordinate> plugins;

    private final List<ProtoCoordinate> managedPlugins;

    private final List<ProtoCoordinate> extensions;

    private final List<ProtoPluginDependency> pluginDependencies;

    private RProjectRelationships( final List<ProtoCoordinate> ancestry, final List<ProtoDependency> dependencies,
                                   final List<ProtoDependency> managedDependencies,
                                   final List<ProtoCoordinate> plugins, final List<ProtoCoordinate> managedPlugins,
                                   final List<ProtoCoordinate> extensions,
                                   final List<ProtoPluginDependency> pluginDependencies )
    {
        this.ancestry = ancestry;
        this.dependencies = dependencies;
        this.managedDependencies = managedDependencies;
        this.plugins = plugins;
        this.managedPlugins = managedPlugins;
        this.extensions = extensions;
        this.pluginDependencies = pluginDependencies;
    }
}
