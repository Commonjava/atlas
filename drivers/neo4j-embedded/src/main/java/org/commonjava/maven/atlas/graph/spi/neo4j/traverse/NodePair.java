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
