package org.commonjava.maven.atlas.spi.neo4j.effective.traverse;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.GraphView;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;

@SuppressWarnings( "rawtypes" )
public class EndGAVsPathsCollector
    extends AbstractAtlasCollector<Path>
{

    private final Set<ProjectVersionRef> endRefs;

    public EndGAVsPathsCollector( final Node start, final ProjectVersionRef end, final GraphView view,
                                  final boolean checkExistence )
    {
        this( Collections.singleton( start ), Collections.singleton( end ), view, checkExistence );
    }

    public EndGAVsPathsCollector( final Set<Node> startNodes, final Set<ProjectVersionRef> endRefs,
                                  final GraphView view, final boolean checkExistence )
    {
        super( startNodes, view, checkExistence );
        this.endRefs = endRefs;
    }

    private EndGAVsPathsCollector( final Set<Node> startNodes, final Set<ProjectVersionRef> endRefs,
                                   final GraphView view, final boolean checkExistence, final Direction direction )
    {
        super( startNodes, view, checkExistence, direction );
        this.endRefs = endRefs;
    }

    @Override
    public PathExpander reverse()
    {
        return new EndGAVsPathsCollector( startNodes, endRefs, view, checkExistence, direction.reverse() );
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
        if ( !end.hasProperty( Conversions.GAV ) )
        {
            return false;
        }

        final ProjectVersionRef ref = Conversions.toProjectVersionRef( end );
        if ( endRefs.contains( ref ) )
        {
            if ( accept( path ) )
            {
                found.add( path );
            }

            return false;
        }

        return true;
    }

}
