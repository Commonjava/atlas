package org.apache.maven.graph.raw.ref;

public class ProtoPluginDependency
    extends ProtoCoordinate
{

    private final ProtoCoordinate plugin;

    private final String type;

    private final String classifier;

    private final String scope;

    private final boolean optional;

    public ProtoPluginDependency( final ProtoCoordinate plugin, final String groupId, final String artifactId,
                                  final String version, final String type, final String classifier, final String scope,
                                  final boolean optional )
    {
        super( groupId, artifactId, version );

        this.plugin = plugin;
        this.type = type;
        this.classifier = classifier;
        this.scope = scope;
        this.optional = optional;
    }

    public final String getType()
    {
        return type;
    }

    public final String getClassifier()
    {
        return classifier;
    }

    public final String getScope()
    {
        return scope;
    }

    public final boolean isOptional()
    {
        return optional;
    }

    public final ProtoCoordinate getPlugin()
    {
        return plugin;
    }

}
