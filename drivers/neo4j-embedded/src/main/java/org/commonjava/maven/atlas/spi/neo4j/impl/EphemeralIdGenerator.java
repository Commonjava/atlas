package org.commonjava.maven.atlas.spi.neo4j.impl;

/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
 */

import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.kernel.IdGeneratorFactory;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.store.IdGenerator;
import org.neo4j.kernel.impl.nioneo.store.IdRange;

public class EphemeralIdGenerator
    implements IdGenerator
{
    public static class Factory
        implements IdGeneratorFactory
    {
        private final Map<IdType, IdGenerator> generators = new EnumMap<IdType, IdGenerator>( IdType.class );

        public IdGenerator open( final FileSystemAbstraction fs, final String fileName, final int grabSize,
                                 final IdType idType, final long highestIdInUse, final boolean startup )
        {
            IdGenerator generator = generators.get( idType );
            if ( generator == null )
            {
                generator = new EphemeralIdGenerator( idType );
                generators.put( idType, generator );
            }
            return generator;
        }

        public void create( final FileSystemAbstraction fs, final String fileName )
        {
        }

        public IdGenerator get( final IdType idType )
        {
            return generators.get( idType );
        }
    }

    private final AtomicLong nextId = new AtomicLong();

    private final IdType idType;

    private final Queue<Long> freeList;

    private final AtomicInteger freedButNotReturnableIdCount = new AtomicInteger();

    public EphemeralIdGenerator( final IdType idType )
    {
        this.idType = idType;
        this.freeList = idType != null && idType.allowAggressiveReuse() ? new ConcurrentLinkedQueue<Long>() : null;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[" + idType + "]";
    }

    public long nextId()
    {
        if ( freeList != null )
        {
            final Long id = freeList.poll();
            if ( id != null )
            {
                return id.longValue();
            }
        }
        return nextId.getAndIncrement();
    }

    public IdRange nextIdBatch( final int size )
    {
        throw new UnsupportedOperationException();
    }

    public void setHighId( final long id )
    {
        nextId.set( id );
    }

    public long getHighId()
    {
        return nextId.get();
    }

    public void freeId( final long id )
    {
        if ( freeList != null )
        {
            freeList.add( id );
        }
        else
        {
            freedButNotReturnableIdCount.getAndIncrement();
        }
    }

    public void close( final boolean shutdown )
    {
    }

    public long getNumberOfIdsInUse()
    {
        final long result = freeList == null ? nextId.get() : nextId.get() - freeList.size();
        return result - freedButNotReturnableIdCount.get();
    }

    public long getDefragCount()
    {
        return 0;
    }

    public void delete()
    {
    }
}