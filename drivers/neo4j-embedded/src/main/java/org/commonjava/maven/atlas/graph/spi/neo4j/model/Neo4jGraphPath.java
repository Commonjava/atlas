package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.spi.model.GraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

public class Neo4jGraphPath
    implements GraphPath<Long>
{

    private final long[] nodes;

    public Neo4jGraphPath( final long... nodeIds )
    {
        this.nodes = nodeIds;
    }

    public Neo4jGraphPath( final Neo4jGraphPath parent, final long id )
    {
        if ( parent == null )
        {
            nodes = new long[] { id };
        }
        else
        {
            nodes = new long[parent.nodes.length + 1];
            System.arraycopy( parent.nodes, 0, nodes, 0, parent.nodes.length );
            nodes[parent.nodes.length] = id;
        }
    }

    public Neo4jGraphPath( final Path path )
    {
        final List<Long> ids = new ArrayList<Long>();
        for ( final Node n : path.nodes() )
        {
            ids.add( n.getId() );
        }

        this.nodes = new long[ids.size()];
        for ( int i = 0; i < ids.size(); i++ )
        {
            this.nodes[i] = ids.get( i );
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( nodes );
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
        final Neo4jGraphPath other = (Neo4jGraphPath) obj;
        if ( !Arrays.equals( nodes, other.nodes ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public Iterator<Long> iterator()
    {
        return new Iterator<Long>()
        {
            private int next = 0;

            @Override
            public boolean hasNext()
            {
                return nodes.length > next;
            }

            @Override
            public Long next()
            {
                return nodes[next++];
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException( "Immutable array of node ID's. Remove not supported." );
            }
        };
    }

    @Override
    public String toString()
    {
        return String.format( "Neo4jGraphPath [nodes=%s]", Arrays.toString( nodes ) );
    }

}
