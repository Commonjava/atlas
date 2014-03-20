package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;

public class CyclePath
    extends Neo4jGraphPath
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

            throw new IndexOutOfBoundsException( next + " is the next index, but the array has only " + ids.length + " items!" );
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException( "Cannot remove id; CyclePath is immutable." );
        }

    }

    private int entryPoint = 0;

    public CyclePath( final long[] ids )
    {
        super( ids );
    }

    public CyclePath( final List<Long> ids )
    {
        super( ids );
    }

    public void setEntryPoint( final long entryPoint )
    {
        final long[] ids = super.getRelationshipIds();
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
        return new CycleIterator( super.getRelationshipIds(), entryPoint );
    }

    @Override
    public long getLastRelationshipId()
    {
        final long[] ids = super.getRelationshipIds();

        final int last = entryPoint > 0 ? entryPoint - 1 : ids.length - 1;

        return ids[last];
    }

    @Override
    public long getFirstRelationshipId()
    {
        return super.getRelationshipIds()[entryPoint];
    }

    @Override
    public long[] getRelationshipIds()
    {
        //        if ( entryPoint == 0 )
        //        {
        //            return getRawRelationshipIds();
        //        }

        final long[] ids = new long[getRawRelationshipIds().length];
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

        final long[] ids = super.getRelationshipIds();
        if ( ids.length == 0 )
        {
            return prime;
        }

        final int entry = findIdentityEntryPoint();
        final CycleIterator it = new CycleIterator( ids, entry );

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

    private int findIdentityEntryPoint()
    {
        final long[] ids = super.getRelationshipIds();
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

        return entry;
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

        final long[] ids = getRawRelationshipIds();
        final long[] oids = other.getRawRelationshipIds();
        if ( ids.length != oids.length )
        {
            return false;
        }
        else if ( ids.length == 0 )
        {
            return true;
        }

        final int entry = findIdentityEntryPoint();
        final int oEntry = other.findIdentityEntryPoint();

        final CycleIterator it = new CycleIterator( ids, entry );
        final CycleIterator oit = new CycleIterator( oids, oEntry );

        while ( it.hasNext() )
        {
            if ( it.next() != oit.next() )
            {
                return false;
            }
        }

        return true;
    }

    private long[] getRawRelationshipIds()
    {
        return super.getRelationshipIds();
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

}
