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
package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class ParentFilter
    extends AbstractTypedFilter
{

    private final boolean allowTerminalParent;

    public ParentFilter()
    {
        this( true );
    }

    public ParentFilter( final boolean allowTerminalParent )
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

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "PARENTS" );
        if ( allowTerminalParent )
        {
            sb.append( "[include logical terminal-parent]" );
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
