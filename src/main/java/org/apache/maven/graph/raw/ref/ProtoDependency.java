package org.apache.maven.graph.raw.ref;

public class ProtoDependency
    extends ProtoCoordinate
{

    private final String type;

    private final String classifier;

    private final String scope;

    private final boolean optional;

    public ProtoDependency( final String groupId, final String artifactId, final String version, final String type,
                            final String classifier, final String scope, final boolean optional )
    {
        super( groupId, artifactId, version );

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

}
