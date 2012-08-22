package org.apache.maven.graph.effective.util;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.VersionedProjectRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;

public final class EGraphUtils
{

    private EGraphUtils()
    {
    }

    public static ExtensionRelationship extension( final VersionedProjectRef owner, final String groupId,
                                                   final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return new ExtensionRelationship( owner, projectVersion( groupId, artifactId, version ), index );
    }

    public static PluginRelationship plugin( final VersionedProjectRef owner, final String groupId,
                                             final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return plugin( owner, groupId, artifactId, version, index, false );
    }

    public static PluginRelationship plugin( final VersionedProjectRef owner, final String groupId,
                                             final String artifactId, final String version, final int index,
                                             final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( owner, projectVersion( groupId, artifactId, version ), index, managed );
    }

    public static PluginRelationship plugin( final VersionedProjectRef owner, final VersionedProjectRef plugin,
                                             final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginRelationship( owner, plugin, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final VersionedProjectRef owner,
                                                                 final ProjectRef plugin, final String groupId,
                                                                 final String artifactId, final String version,
                                                                 final int index )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( owner, plugin, groupId, artifactId, version, null, null, index, false );
    }

    public static PluginDependencyRelationship pluginDependency( final VersionedProjectRef owner,
                                                                 final ProjectRef plugin, final String groupId,
                                                                 final String artifactId, final String version,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return pluginDependency( owner, plugin, groupId, artifactId, version, null, null, index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final VersionedProjectRef owner,
                                                                 final ProjectRef plugin, final String groupId,
                                                                 final String artifactId, final String version,
                                                                 final String type, final String classifier,
                                                                 final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( owner, plugin, artifact( groupId, artifactId, version, type,
                                                                          classifier, false ), index, managed );
    }

    public static PluginDependencyRelationship pluginDependency( final VersionedProjectRef owner,
                                                                 final ProjectRef plugin,
                                                                 final VersionedProjectRef dep, final String type,
                                                                 final String classifier, final int index,
                                                                 final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new PluginDependencyRelationship( owner, plugin, artifact( dep, type, classifier, false ), index,
                                                 managed );
    }

    public static DependencyRelationship dependency( final VersionedProjectRef owner, final String groupId,
                                                     final String artifactId, final String version, final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( owner, groupId, artifactId, version, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final VersionedProjectRef owner, final VersionedProjectRef dep,
                                                     final int index )
        throws InvalidVersionSpecificationException
    {
        return dependency( owner, dep, null, null, false, null, index, false );
    }

    public static DependencyRelationship dependency( final VersionedProjectRef owner, final String groupId,
                                                     final String artifactId, final String version,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return dependency( owner, groupId, artifactId, version, null, null, false, scope, index, managed );
    }

    public static DependencyRelationship dependency( final VersionedProjectRef owner, final VersionedProjectRef dep,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( owner, artifact( dep, null, null, false ), scope, index, managed );
    }

    public static DependencyRelationship dependency( final VersionedProjectRef owner, final String groupId,
                                                     final String artifactId, final String version, final String type,
                                                     final String classifier, final boolean optional,
                                                     final DependencyScope scope, final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( owner, artifact( groupId, artifactId, version, type, classifier, optional ),
                                           null, index, false );
    }

    public static DependencyRelationship dependency( final VersionedProjectRef owner, final VersionedProjectRef dep,
                                                     final String type, final String classifier,
                                                     final boolean optional, final DependencyScope scope,
                                                     final int index, final boolean managed )
        throws InvalidVersionSpecificationException
    {
        return new DependencyRelationship( owner, artifact( dep, type, classifier, optional ), null, index, false );
    }

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( projectVersion( groupId, artifactId, version ), null, null, false );
    }

    public static ArtifactRef artifact( final VersionedProjectRef ref )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( ref, null, null, false );
    }

    public static ArtifactRef artifact( final String groupId, final String artifactId, final String version,
                                        final String type, final String classifier, final boolean optional )
        throws InvalidVersionSpecificationException
    {
        return new ArtifactRef( projectVersion( groupId, artifactId, version ), type, classifier, optional );
    }

    private static ArtifactRef artifact( final VersionedProjectRef dep, final String type, final String classifier,
                                         final boolean optional )
    {
        return new ArtifactRef( dep, type, classifier, optional );
    }

    public static VersionedProjectRef projectVersion( final String groupId, final String artifactId,
                                                      final String version )
        throws InvalidVersionSpecificationException
    {
        return new VersionedProjectRef( groupId, artifactId, version );
    }

}
