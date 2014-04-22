/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
