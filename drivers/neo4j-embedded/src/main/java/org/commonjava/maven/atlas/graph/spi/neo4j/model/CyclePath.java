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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public class CyclePath
    implements Iterable<Long>
{

    public static final class CycleIterator
        implements Iterator<Long>
    {

        private final long[] ids;

        private int next;

        private final int start;

        public CycleIterator( final long[] ids, final int entryPoint )
        {
            this.ids = ids;
            start = entryPoint;
            next = entryPoint;
        }

        @Override
        public boolean hasNext()
        {
            if ( next >= ids.length )
            {
                if ( start > 0 )
                {
                    next = 0;
                    return true;
                }

                return false;
            }
            else if ( next == -1 )
            {
                return false;
            }

            return true;
        }

        @Override
        public Long next()
        {
            if ( hasNext() )
            {
                final long id = ids[next++];
                if ( next == start )
                {
                    next = -1;
                }

                return id;
            }
            else if ( next == start )
            {
                throw new IndexOutOfBoundsException( next + " is the starting point for this iteration!" );
            }

            throw new IndexOutOfBoundsException( next + " is the next index, but the array has only " + ids.length
                + " items!" );
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException( "Cannot remove id; CyclePath is immutable." );
        }

    }

    private int entryPoint = 0;

    private final long[] ids;

    public CyclePath( final long[] ids )
    {
        this.ids = ids;
    }

    public CyclePath( final List<Long> ids )
    {
        this.ids = new long[ids.size()];
        for ( int i = 0; i < ids.size(); i++ )
        {
            this.ids[i] = ids.get( i );
        }
    }

    public CyclePath( final Path path )
    {
        final List<Long> ids = new ArrayList<Long>();
        for ( final Relationship r : path.relationships() )
        {
            ids.add( r.getId() );
        }

        this.ids = new long[ids.size()];
        for ( int i = 0; i < ids.size(); i++ )
        {
            this.ids[i] = ids.get( i );
        }
    }

    public void setEntryPoint( final long entryPoint )
    {
        for ( int i = 0; i < ids.length; i++ )
        {
            if ( ids[i] == entryPoint )
            {
                this.entryPoint = i;
                return;
            }
        }
    }

    public void clearEntryPoint()
    {
        entryPoint = 0;
    }

    @Override
    public Iterator<Long> iterator()
    {
        return new CycleIterator( ids, entryPoint );
    }

    public long getLastRelationshipId()
    {
        final int last = entryPoint > 0 ? entryPoint - 1 : ids.length - 1;

        return ids[last];
    }

    public long getFirstRelationshipId()
    {
        return ids[entryPoint];
    }

    public long[] getRelationshipIds()
    {
        //        if ( entryPoint == 0 )
        //        {
        //            return getRawRelationshipIds();
        //        }

        final long[] ids = new long[this.ids.length];
        final Iterator<Long> iterator = iterator();

        int i = 0;
        while ( iterator.hasNext() )
        {
            ids[i++] = iterator.next();
        }

        return ids;
    }

    @Override
    public int hashCode()
    {
        final int prime = 37;

        if ( ids.length == 0 )
        {
            return prime;
        }

        final CycleIterator it = identityIterator();

        int result = prime;

        int i = 0;
        while ( it.hasNext() )
        {
            if ( i % 2 == 1 )
            {
                result += Long.valueOf( it.next() )
                              .hashCode();
            }
            else
            {
                result -= Long.valueOf( it.next() )
                              .hashCode();
            }

            i++;
        }

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
        final CyclePath other = (CyclePath) obj;

        if ( ids.length != other.ids.length )
        {
            return false;
        }
        else if ( ids.length == 0 )
        {
            return true;
        }

        final CycleIterator it = identityIterator();
        final CycleIterator oit = other.identityIterator();

        while ( it.hasNext() )
        {
            if ( it.next() != oit.next() )
            {
                return false;
            }
        }

        return true;
    }

    public CycleIterator identityIterator()
    {
        final long[] sorted = new long[ids.length];

        System.arraycopy( ids, 0, sorted, 0, ids.length );
        Arrays.sort( sorted );

        int entry = 0;
        for ( int i = 0; i < ids.length; i++ )
        {
            if ( ids[i] == sorted[0] )
            {
                entry = i;
                break;
            }
        }

        return new CycleIterator( ids, entry );
    }

    private long[] getRawRelationshipIds()
    {
        return ids;
    }

    public CyclePath reorientToEntryPoint()
    {
        if ( entryPoint == 0 )
        {
            return this;
        }

        final long[] ids = getRawRelationshipIds();
        final long[] reoriented = new long[ids.length];

        final CycleIterator it = new CycleIterator( ids, entryPoint );
        int i = 0;
        while ( it.hasNext() )
        {
            reoriented[i++] = it.next();
        }

        return new CyclePath( reoriented );
    }

    public String getKey()
    {
        final StringBuilder sb = new StringBuilder();

        final CycleIterator it = identityIterator();
        while ( it.hasNext() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }

            sb.append( it.next() );
        }

        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "CyclePath [" + getKey() + "]";
    }

    public int length()
    {
        return ids.length;
    }

}
