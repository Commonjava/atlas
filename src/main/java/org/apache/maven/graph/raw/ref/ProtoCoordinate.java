package org.apache.maven.graph.raw.ref;

public class ProtoCoordinate
{

    private final String groupId;

    private final String artifactId;

    private final String version;

    public ProtoCoordinate( final String groupId, final String artifactId, final String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public final String getGroupId()
    {
        return groupId;
    }

    public final String getArtifactId()
    {
        return artifactId;
    }

    public final String getVersion()
    {
        return version;
    }

}
