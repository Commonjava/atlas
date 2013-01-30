/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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

    String getVersionString();

    T selectVersion( SingleVersion version );

}
