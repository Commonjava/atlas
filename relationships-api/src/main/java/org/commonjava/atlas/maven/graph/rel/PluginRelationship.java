/**
 * Copyright (C) 2012 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.graph.rel;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface PluginRelationship
        extends ProjectRelationship<PluginRelationship, ProjectVersionRef>,Serializable
{
    boolean isReporting();

    @Override
    PluginRelationship cloneFor( ProjectVersionRef projectRef );

    @Override
    ArtifactRef getTargetArtifact();

    @Override
    PluginRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    PluginRelationship selectTarget( ProjectVersionRef ref );
}
