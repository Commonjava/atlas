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
package org.commonjava.maven.atlas.ident.ref;

import java.io.Serializable;

import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;

public interface VersionedRef<T>
    extends Serializable
{

    boolean isSpecificVersion();

    boolean isRelease();

    boolean isSnapshot();

    boolean isCompound();

    boolean matchesVersion( SingleVersion version );

    VersionSpec getVersionSpec();

    String getVersionString();

    T selectVersion( String version );

    T selectVersion( String version, boolean force );

    T selectVersion( SingleVersion version );

    T selectVersion( SingleVersion version, boolean force );

}
