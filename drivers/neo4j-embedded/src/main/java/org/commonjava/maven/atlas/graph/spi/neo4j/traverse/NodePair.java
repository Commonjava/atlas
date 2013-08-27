package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import org.neo4j.graphdb.Node;

public final class NodePair
{
    private final Node from;

    private final Node to;

    public NodePair( final Node from, final Node to )
    {
        this.from = from;
        this.to = to;
    }

    public Node getFrom()
    {
        return from;
    }

    public Node getTo()
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

        return o.from.getId() == from.getId() && o.to.getId() == to.getId();
    }

    @Override
    public int hashCode()
    {
        final int result = (int) ( 13 * from.getId() + to.getId() );

        return result;
    }

    @Override
    public String toString()
    {
        return String.format( "NodePair %d -> %d (%d)", from.getId(), to.getId(), hashCode() );
    }
}