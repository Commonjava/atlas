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
package org.commonjava.maven.atlas.ident.ref;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface ProjectRef
        extends Serializable, Comparable<ProjectRef>
{
    String getGroupId();

    String getArtifactId();

    ProjectRef asProjectRef();

    VersionlessArtifactRef asVersionlessPomArtifact();

    VersionlessArtifactRef asVersionlessJarArtifact();

    VersionlessArtifactRef asVersionlessArtifactRef( String type, String classifier );

    VersionlessArtifactRef asVersionlessArtifactRef( String type, String classifier, boolean optional );

    VersionlessArtifactRef asVersionlessArtifactRef( TypeAndClassifier tc );

    VersionlessArtifactRef asVersionlessArtifactRef( TypeAndClassifier tc, boolean optional );

    boolean matches( ProjectRef ref );
}
