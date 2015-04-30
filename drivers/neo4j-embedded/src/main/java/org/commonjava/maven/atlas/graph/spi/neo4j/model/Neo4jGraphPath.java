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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public class Neo4jGraphPath
    implements GraphPath<Long>
{

    private final long[] relationships;

    private final long startNode;

    private final long endNode;

    public Neo4jGraphPath( final Neo4jGraphPath parent, final Relationship... relationships )
    {
        if ( parent == null )
        {
            throw new NullPointerException( "Parent path cannot be null" );
        }

        this.startNode = parent.startNode;
        if ( relationships.length > 0 )
        {
            this.endNode = relationships[relationships.length - 1].getEndNode()
                                                                  .getId();
        }
        else
        {
            this.endNode = parent.endNode;
        }

        final int parentLen = parent.relationships.length;

        this.relationships = new long[parentLen + relationships.length];

        if ( parentLen > 0 )
        {
            System.arraycopy( parent.relationships, 0, this.relationships, 0, parent.relationships.length );
        }

        if ( this.relationships.length > 0 )
        {
            for ( int i = parentLen; i < this.relationships.length; i++ )
            {
                this.relationships[i] = relationships[i - parentLen].getId();
            }
        }
    }

    public Neo4jGraphPath( final Path path )
    {
        this.startNode = path.startNode()
                             .getId();
        this.endNode = path.endNode()
                           .getId();

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

    public Neo4jGraphPath( final Node start, final Node end, final long[] rids )
    {
        this.startNode = start.getId();
        this.endNode = end.getId();
        this.relationships = rids;
    }

    public Neo4jGraphPath( final Relationship[] relationships )
    {
        if ( relationships.length > 0 )
        {
            this.startNode = relationships[0].getStartNode()
                                             .getId();

            this.endNode = relationships[relationships.length - 1].getEndNode()
                                                                  .getId();
        }
        else
        {
            throw new IllegalArgumentException(
                                                "Cannot initialize path with zero relationships and no explicit start node!" );
        }

        this.relationships = new long[relationships.length];

        final int i = 0;
        for ( final Relationship relationship : relationships )
        {
            this.relationships[i] = relationship.getId();
        }
    }

    private Neo4jGraphPath( final Neo4jGraphPath parent, final long endNode, final long[] newRelationships )
    {
        this.startNode = parent.startNode;
        this.endNode = endNode;

        final int parentLen = parent.relationships.length;

        this.relationships = new long[parentLen + newRelationships.length];

        System.arraycopy( parent.relationships, 0, this.relationships, 0, parentLen );
        System.arraycopy( newRelationships, 0, this.relationships, parentLen, newRelationships.length );
    }

    public Neo4jGraphPath append( final Neo4jGraphPath childPath )
    {
        if ( length() > 0 && getLastRelationshipId() != childPath.getFirstRelationshipId() )
        {
            throw new IllegalArgumentException( "Cannot splice " + childPath + " onto " + this
                + ". They don't overlap on last/first relationshipId!" );
        }

        if ( childPath.length() < 2 )
        {
            return this;
        }

        final long[] ids = new long[childPath.length() - 1];
        System.arraycopy( childPath.getRelationshipIds(), 1, ids, 0, ids.length );

        return new Neo4jGraphPath( this, childPath.endNode, ids );
    }

    public long getStartNodeId()
    {
        return startNode;
    }

    public long getEndNodeId()
    {
        return endNode;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Long.valueOf( startNode ).hashCode();
        result = prime * result + Long.valueOf( endNode ).hashCode();
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
        if ( startNode != other.startNode )
        {
            return false;
        }
        if ( endNode != other.endNode )
        {
            return false;
        }
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
        return String.format( "%s [relationships=%s, from=%s, to=%s]", getClass().getSimpleName(),
                              Arrays.toString( relationships ), startNode, endNode );
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

    public long getFirstRelationshipId()
    {
        if ( relationships.length < 1 )
        {
            return -1;
        }

        return relationships[0];
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
