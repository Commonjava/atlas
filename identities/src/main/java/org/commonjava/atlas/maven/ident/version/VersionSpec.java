/**
 * Copyright (C) 2012-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.ident.version;

import java.io.Serializable;

public interface VersionSpec
    extends Comparable<VersionSpec>, Serializable
{

    boolean isSnapshot();

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

    boolean isRelease();
}
