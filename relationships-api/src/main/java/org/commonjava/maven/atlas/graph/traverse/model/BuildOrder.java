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
package org.commonjava.maven.atlas.graph.traverse.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public final class BuildOrder
{

    private final List<ProjectRef> order;

    private final Set<EProjectCycle> cycles;

    public BuildOrder( final List<ProjectRef> order, final Set<EProjectCycle> cycles )
    {
        this.order = Collections.unmodifiableList( order );
        this.cycles = cycles == null ? null : Collections.unmodifiableSet( cycles );
    }

    public List<ProjectRef> getOrder()
    {
        return order;
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

}
