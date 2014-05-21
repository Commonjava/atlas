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
package org.commonjava.maven.atlas.graph.model;

import java.io.Serializable;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.VersionManagerMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class GraphPathInfo
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    private transient GraphView view;

    public GraphPathInfo( final GraphView view )
    {
        this.view = view;
        filter = view.getFilter();
        mutator = view.getMutator() == null ? new VersionManagerMutator() : view.getMutator();
    }

    public GraphPathInfo( final ProjectRelationshipFilter filter, final GraphMutator mutator, final GraphView view )
    {
        this.filter = filter;
        this.mutator = mutator;
        this.view = view;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public GraphMutator getMutator()
    {
        return mutator;
    }

    public ProjectRelationship<?> selectRelationship( ProjectRelationship<?> next, final GraphPath<?> path )
    {
        if ( filter != null && !filter.accept( next ) )
        {
            return null;
        }

        if ( mutator != null )
        {
            next = mutator.selectFor( next, path, view );
        }

        return next;
    }

    public GraphPathInfo getChildPathInfo( final ProjectRelationship<?> rel )
    {
        final ProjectRelationshipFilter nextFilter = filter == null ? null : filter.getChildFilter( rel );
        final GraphMutator nextMutator = mutator == null ? null : mutator.getMutatorFor( rel, view );
        if ( nextFilter == filter && nextMutator == mutator )
        {
            return this;
        }

        return new GraphPathInfo( nextFilter, nextMutator, view );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( filter == null ) ? 0 : filter.hashCode() );
        result = prime * result + ( ( mutator == null ) ? 0 : mutator.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final GraphPathInfo other = (GraphPathInfo) obj;
        if ( filter == null )
        {
            if ( other.filter != null )
            {
                return false;
            }
        }
        else if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        if ( mutator == null )
        {
            if ( other.mutator != null )
            {
                return false;
            }
        }
        else if ( !mutator.equals( other.mutator ) )
        {
            return false;
        }
        return true;
    }

    public String getKey()
    {
        return ( filter == null ? "none" : filter.getCondensedId() ) + "/"
            + ( mutator == null ? "none" : mutator.getCondensedId() );
    }

    public void reattach( final GraphView view )
    {
        this.view = view;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphPathInfo [filter=%s, mutator=%s, view=%s]", filter, mutator, view.getShortId() );
    }

}
