package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;

@SuppressWarnings( "rawtypes" )
public class ConnectingPathsCollector
    extends AbstractAtlasCollector<Path>
{

    private final Set<Node> endNodes;

    public ConnectingPathsCollector( final Node start, final Node end, final GraphView view, final Node wsNode, final boolean checkExistence )
    {
        this( Collections.singleton( start ), Collections.singleton( end ), view, wsNode, checkExistence );
    }

    public ConnectingPathsCollector( final Set<Node> startNodes, final Set<Node> endNodes, final GraphView view, final Node wsNode,
                                     final boolean checkExistence )
    {
        super( startNodes, view, wsNode, checkExistence );
        this.endNodes = endNodes;
    }

    private ConnectingPathsCollector( final Set<Node> startNodes, final Set<Node> endNodes, final GraphView view, final Node wsNode,
                                      final boolean checkExistence, final Direction direction )
    {
        super( startNodes, view, wsNode, checkExistence, direction );
        this.endNodes = endNodes;
    }

    @Override
    public PathExpander reverse()
    {
        return new ConnectingPathsCollector( startNodes, endNodes, view, wsNode, checkExistence, direction.reverse() );
    }

    public boolean hasFoundPaths()
    {
        return !found.isEmpty();
    }

    public Set<Path> getFoundPaths()
    {
        return found;
    }

    @Override
    public Iterator<Path> iterator()
    {
        return found.iterator();
    }

    @Override
    protected boolean returnChildren( final Path path )
    {
        final Node end = path.endNode();
        if ( endNodes.contains( end ) )
        {
            if ( accept( path ) )
            {
                //                logger.info( "FOUND path ending in: %s", endId );
                found.add( path );
            }

            return false;
        }

        return true;
    }

}
