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
