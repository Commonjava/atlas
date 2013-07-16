/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.common.version;

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
