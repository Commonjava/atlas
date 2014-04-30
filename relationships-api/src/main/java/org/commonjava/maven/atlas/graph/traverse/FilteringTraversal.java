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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

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
                                          final List<ProjectRelationship<?>> path )
    {
        if ( doCapture )
        {
            captured.add( relationship );
        }
        return true;
    }

}
