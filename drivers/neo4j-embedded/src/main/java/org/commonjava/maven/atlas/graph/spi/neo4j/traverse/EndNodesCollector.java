package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;

@SuppressWarnings( "rawtypes" )
public class EndNodesCollector
    extends AbstractAtlasCollector<Node>
{

    private final Set<Node> endNodes;

    public EndNodesCollector( final Node start, final Node end, final GraphView view, final Node wsNode, final boolean checkExistence )
    {
        this( Collections.singleton( start ), Collections.singleton( end ), view, wsNode, checkExistence );
    }

    public EndNodesCollector( final Set<Node> startNodes, final Set<Node> endNodes, final GraphView view, final Node wsNode,
                              final boolean checkExistence )
    {
        super( startNodes, view, wsNode, checkExistence );
        this.endNodes = endNodes;
        logger.debug( "Collector: start=(%s), end=(%s), view=(%s), checkExistence=%s", join( startNodes, ", " ), join( endNodes, ", " ), view,
                      checkExistence );
        //        this.logEnabled = true;
    }

    private EndNodesCollector( final Set<Node> startNodes, final Set<Node> endNodes, final GraphView view, final Node wsNode,
                               final boolean checkExistence, final Direction direction )
    {
        super( startNodes, view, wsNode, checkExistence, direction );
        this.endNodes = endNodes;
        //        this.logEnabled = true;
    }

    @Override
    public PathExpander reverse()
    {
        return new EndNodesCollector( startNodes, endNodes, view, wsNode, checkExistence, direction.reverse() );
    }

    public boolean hasFoundNodes()
    {
        return !found.isEmpty();
    }

    public Set<Node> getFoundNodes()
    {
        return found;
    }

    @Override
    public Iterator<Node> iterator()
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
                found.add( end );
            }

            return false;
        }

        return true;
    }

}
