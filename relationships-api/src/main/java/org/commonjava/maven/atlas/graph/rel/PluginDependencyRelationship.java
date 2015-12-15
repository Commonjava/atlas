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
package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface PluginDependencyRelationship
        extends ProjectRelationship<PluginDependencyRelationship, ArtifactRef>,Serializable
{
    ProjectRef getPlugin();

    @Override
    PluginDependencyRelationship cloneFor( ProjectVersionRef projectRef );

    @Override
    ArtifactRef getTargetArtifact();

    @Override
    PluginDependencyRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    PluginDependencyRelationship selectTarget( ProjectVersionRef ref );

    boolean isOptional();
}
