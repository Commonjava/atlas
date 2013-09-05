package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;

@SuppressWarnings( "rawtypes" )
public class RootedRelationshipsCollector
    extends AbstractAtlasCollector<Relationship>
{

    public RootedRelationshipsCollector( final Node start, final GraphView view, final Node wsNode, final boolean checkExistence )
    {
        super( start, view, wsNode, checkExistence );
    }

    public RootedRelationshipsCollector( final Set<Node> startNodes, final GraphView view, final Node wsNode, final boolean checkExistence )
    {
        super( startNodes, view, wsNode, checkExistence );
        //        this.logEnabled = true;
    }

    private RootedRelationshipsCollector( final Set<Node> startNodes, final GraphView view, final Node wsNode, final boolean checkExistence,
                                          final Direction direction )
    {
        super( startNodes, view, wsNode, checkExistence, direction );
        //        this.logEnabled = true;
    }

    @Override
    public PathExpander reverse()
    {
        return new RootedRelationshipsCollector( startNodes, view, wsNode, checkExistence, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Set<Relationship> getFoundRelationships()
    {
        return found;
    }

    @Override
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

            return true;
        }

        return false;
    }

}
