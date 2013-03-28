package org.commonjava.maven.atlas.spi.neo4j.effective.traverse;

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;

@SuppressWarnings( "rawtypes" )
public class RootedRelationshipsCollector
    extends AbstractAtlasCollector<Relationship>
{

    public RootedRelationshipsCollector( final Node start, final ProjectRelationshipFilter filter,
                                         final boolean checkExistence )
    {
        super( start, filter, checkExistence );
    }

    public RootedRelationshipsCollector( final Set<Node> startNodes, final ProjectRelationshipFilter filter,
                                         final boolean checkExistence )
    {
        super( startNodes, filter, checkExistence );
    }

    private RootedRelationshipsCollector( final Set<Node> startNodes, final ProjectRelationshipFilter filter,
                                          final boolean checkExistence, final Direction direction )
    {
        super( startNodes, filter, checkExistence, direction );
    }

    public PathExpander reverse()
    {
        return new RootedRelationshipsCollector( startNodes, filter, checkExistence, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Set<Relationship> getFoundRelationships()
    {
        return found;
    }

    public Iterator<Relationship> iterator()
    {
        return found.iterator();
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        if ( accept( path ) )
        {
            //                logger.info( "FOUND path ending in: %s", endId );
            for ( final Relationship r : path.relationships() )
            {
                found.add( r );
            }
        }

        return true;
    }

}
