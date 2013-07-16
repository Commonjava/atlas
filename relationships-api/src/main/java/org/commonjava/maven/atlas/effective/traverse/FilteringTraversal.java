/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.effective.traverse;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

public class FilteringTraversal
    extends AbstractFilteringTraversal
{

    private final List<ProjectRelationship<?>> captured = new ArrayList<ProjectRelationship<?>>();

    private final boolean doCapture;

    public FilteringTraversal( final ProjectRelationshipFilter filter )
    {
        this( filter, false );
    }

    public FilteringTraversal( final ProjectRelationshipFilter filter, final boolean doCapture )
    {
        super( filter );
        this.doCapture = doCapture;
    }

    public List<ProjectRelationship<?>> getCapturedRelationships()
    {
        return captured;
    }

    public List<ProjectVersionRef> getCapturedProjects( final boolean unique )
    {
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : captured )
        {
            final ProjectVersionRef d = rel.getDeclaring();
            final ProjectVersionRef t = rel.getTarget()
                                           .asProjectVersionRef();

            if ( !unique || !refs.contains( d ) )
            {
                refs.add( d );
            }

            if ( !unique || !refs.contains( t ) )
            {
                refs.add( t );
            }
        }

        return refs;
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( doCapture )
        {
            captured.add( relationship );
        }
        return true;
    }

}
