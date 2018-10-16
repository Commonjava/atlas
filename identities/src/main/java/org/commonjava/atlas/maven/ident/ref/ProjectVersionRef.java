/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.atlas.maven.ident.ref;

import org.commonjava.atlas.maven.ident.version.SingleVersion;
import org.commonjava.atlas.maven.ident.version.VersionSpec;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface ProjectVersionRef
        extends ProjectRef, VersionedRef<ProjectVersionRef>,Serializable
{
    ProjectVersionRef asProjectVersionRef();

    ArtifactRef asPomArtifact();

    ArtifactRef asJarArtifact();

    ArtifactRef asArtifactRef( String type, String classifier );

    ArtifactRef asArtifactRef( TypeAndClassifier tc );

    VersionSpec getVersionSpecRaw();

    String getVersionStringRaw();

    @Override
    boolean isRelease();

    @Override
    boolean isSpecificVersion();

    @Override
    boolean matchesVersion( SingleVersion version );

    @Override
    ProjectVersionRef selectVersion( String version );

    @Override
    ProjectVersionRef selectVersion( String version, boolean force );

    @Override
    ProjectVersionRef selectVersion( SingleVersion version );

    @Override
    ProjectVersionRef selectVersion( SingleVersion version, boolean force );

    ProjectVersionRef newRef( String groupId, String artifactId, SingleVersion version );

    @Override
    VersionSpec getVersionSpec();

    boolean versionlessEquals( ProjectVersionRef other );

    @Override
    boolean isCompound();

    @Override
    boolean isSnapshot();

    @Override
    String getVersionString();

    boolean isVariableVersion();
}
