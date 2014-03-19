/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class ParentFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final ParentFilter EXCLUDE_TERMINAL_PARENTS = new ParentFilter( false );

    public static final ParentFilter INCLUDE_TERMINAL_PARENTS = new ParentFilter( true );

    private final boolean allowTerminalParent;

    private ParentFilter( final boolean allowTerminalParent )
    {
        super( RelationshipType.PARENT, true, false, true );
        this.allowTerminalParent = allowTerminalParent;
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        if ( allowTerminalParent || !( (ParentRelationship) rel ).isTerminus() )
        {
            return true;
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( allowTerminalParent ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ParentFilter other = (ParentFilter) obj;
        if ( allowTerminalParent != other.allowTerminalParent )
        {
            return false;
        }
        return true;
    }

    @Override
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( ",terminalParent:" )
          .append( allowTerminalParent );
    }

}
