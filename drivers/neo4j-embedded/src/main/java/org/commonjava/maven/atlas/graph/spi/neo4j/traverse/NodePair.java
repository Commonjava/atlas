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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import org.neo4j.graphdb.Node;

public final class NodePair
{
    private final long from;

    private final long to;

    public NodePair( final Node from, final Node to )
    {
        this.from = from.getId();
        this.to = to.getId();
    }

    public long getFrom()
    {
        return from;
    }

    public long getTo()
    {
        return to;
    }

    @Override
    public boolean equals( final Object other )
    {
        if ( other == this )
        {
            return true;
        }
        if ( !( other instanceof NodePair ) )
        {
            return false;
        }
        final NodePair o = (NodePair) other;

        return o.from == from && o.to == to;
    }

    @Override
    public int hashCode()
    {
        final int result = (int) ( 1337133713 * from / to );

        return result;
    }

    @Override
    public String toString()
    {
        return String.format( "NodePair %d -> %d (%d)", from, to, hashCode() );
    }
}
