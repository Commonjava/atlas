/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
