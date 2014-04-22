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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class BomFilter
    extends AbstractTypedFilter
{

    private static final long serialVersionUID = 1L;

    public static final BomFilter INSTANCE = new BomFilter();

    private BomFilter()
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive, 
        // but they're structural, so managed isn't quite correct (despite 
        // Maven's unfortunate choice for location).
        super( RelationshipType.BOM, true, false, true );
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        return true;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

}
