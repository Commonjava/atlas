package org.apache.maven.graph.common.version;

public interface MultiVersionSpec
{

    boolean isPinned();

    SingleVersion getPinnedVersion();

    boolean isSnapshot();

    boolean contains( VersionSpec spec );

}
