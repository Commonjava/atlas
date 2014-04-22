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

import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class AndFilter
    extends AbstractAggregatingFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AndFilter( final Collection<? extends ProjectRelationshipFilter> filters )
    {
        super( filters );
    }

    public <T extends ProjectRelationshipFilter> AndFilter( final T... filters )
    {
        super( filters );
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean accepted = true;
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            accepted = accepted && filter.accept( rel );
            if ( !accepted )
            {
                break;
            }
        }

        return accepted;
    }

    @Override
    protected AbstractAggregatingFilter newChildFilter( final List<ProjectRelationshipFilter> childFilters )
    {
        if ( !filtersEqual( childFilters ) )
        {
            return new AndFilter( childFilters );
        }

        return this;
    }

}
