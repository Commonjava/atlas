package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public final class PathKey
{

    private final List<Long> ids;

    public PathKey( final Path path )
    {
        ids = new ArrayList<Long>();
        for ( final Relationship r : path.relationships() )
        {
            ids.add( r.getId() );
        }
    }

    public PathKey( final Path path, final Relationship next )
    {
        this( path );

        ids.add( next.getId() );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( ids == null ) ? 0 : ids.hashCode() );
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
        final PathKey other = (PathKey) obj;
        if ( ids == null )
        {
            if ( other.ids != null )
            {
                return false;
            }
        }
        else if ( !ids.equals( other.ids ) )
        {
            return false;
        }
        return true;
    }

}
