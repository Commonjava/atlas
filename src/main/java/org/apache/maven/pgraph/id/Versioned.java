package org.apache.maven.pgraph.id;

import org.apache.maven.pgraph.version.SingleVersion;
import org.apache.maven.pgraph.version.VersionSpec;

public interface Versioned<T>
{

    boolean isSpecificVersion();

    boolean isRelease();

    boolean isSnapshot();

    boolean isCompound();

    boolean matchesVersion( SingleVersion version );

    VersionSpec getVersionSpec();

    T selectVersion( SingleVersion version );

}
