package org.apache.maven.graph.common.ref;

import java.io.Serializable;

import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionSpec;

public interface VersionedRef<T>
    extends Serializable
{

    boolean isSpecificVersion();

    boolean isRelease();

    boolean isSnapshot();

    boolean isCompound();

    boolean matchesVersion( SingleVersion version );

    VersionSpec getVersionSpec();

    T selectVersion( SingleVersion version );

}
