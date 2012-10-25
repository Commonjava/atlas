package org.apache.maven.graph.common.version;

import java.io.Serializable;

public interface VersionSpec
    extends Comparable<VersionSpec>, Serializable
{

    /**
     * Version is concrete if it cannot be interpreted (resolved). Non-concrete versions include compound versions,
     * version ranges, and snapshots.
     * 
     * @return false if version is compound, a range that is not pinned to a concrete single version, or a snapshot;
     *         otherwise true
     */
    boolean isConcrete();

    /**
     * Version is single if it only contains a single version (even if that version is not concrete). Single versions
     * may contain compound versions consisting of a single pinned range, a pinned range itself, or any
     * {@link SingleVersion} instance.
     * 
     * @return false if version is compound or a range, and contains more than one possible {@link SingleVersion}
     *         version.
     */
    boolean isSingle();

    /**
     * Render the version into the standard Maven version syntax.
     */
    String renderStandard();

    /**
     * Determine whether the given version specification is contained within this version.
     */
    boolean contains( VersionSpec version );

    /**
     * Retrieve the concrete version from this version specification, if it is available (See:
     * {@link VersionSpec#isConcrete()}.
     * 
     * @see VersionSpec#isConcrete()
     */
    SingleVersion getConcreteVersion();

    /**
     * Retrieve the single version from this version specification, if it is available (See:
     * {@link VersionSpec#isSingle()}.
     * 
     * @see VersionSpec#isSingle()
     */
    SingleVersion getSingleVersion();
}
