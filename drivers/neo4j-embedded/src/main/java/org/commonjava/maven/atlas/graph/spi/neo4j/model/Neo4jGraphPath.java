package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public class Neo4jGraphPath
    implements GraphPath<Long>
{

    private final long[] relationships;

    public Neo4jGraphPath( final long... relationshipIds )
    {
        this.relationships = relationshipIds;
    }

    public Neo4jGraphPath( final Neo4jGraphPath parent, final long relationshipId )
    {
        if ( parent == null )
        {
            relationships = new long[] { relationshipId };
        }
        else
        {
            relationships = new long[parent.relationships.length + 1];
            System.arraycopy( parent.relationships, 0, relationships, 0, parent.relationships.length );
            relationships[parent.relationships.length] = relationshipId;
        }
    }

    public Neo4jGraphPath( final Path path )
    {
        final List<Long> ids = new ArrayList<Long>();
        for ( final Relationship r : path.relationships() )
        {
            ids.add( r.getId() );
        }

        this.relationships = new long[ids.size()];
        for ( int i = 0; i < ids.size(); i++ )
        {
            this.relationships[i] = ids.get( i );
        }
    }

    public Neo4jGraphPath( final List<Long> ids )
    {
        this.relationships = new long[ids.size()];
        for ( int i = 0; i < ids.size(); i++ )
        {
            this.relationships[i] = ids.get( i );
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( relationships );
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
        if ( !Arrays.equals( relationships, other.relationships ) )
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
                return relationships.length > next;
            }

            @Override
            public Long next()
            {
                return relationships[next++];
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException( "Immutable array of relationship ID's. Remove not supported." );
            }
        };
    }

    @Override
    public String toString()
    {
        return String.format( "Neo4jGraphPath [relationships=%s]", Arrays.toString( relationships ) );
    }

    @Override
    public String getKey()
    {
        final StringBuilder sb = new StringBuilder();
        for ( final long id : relationships )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }
            sb.append( id );
        }

        return DigestUtils.shaHex( sb.toString() );
    }

    public long getLastRelationshipId()
    {
        if ( relationships.length < 1 )
        {
            return -1;
        }

        return relationships[relationships.length - 1];
    }

    public long[] getRelationshipIds()
    {
        return relationships;
    }

    public int length()
    {
        return relationships.length;
    }

    public boolean contains( final long id )
    {
        for ( final long rid : relationships )
        {
            if ( rid == id )
            {
                return true;
            }
        }
        return false;
    }

}
