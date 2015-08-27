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
package org.commonjava.maven.atlas.graph.model;

import java.io.Serializable;
import java.util.Collection;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public interface EProjectRelationshipCollection
    extends Serializable
{

    Collection<ProjectRelationship<?, ?>> getAllRelationships();

    Collection<ProjectRelationship<?, ?>> getExactAllRelationships();

}
