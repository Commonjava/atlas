package org.commonjava.maven.atlas.spi.neo4j.effective.traverse;

import static org.commonjava.maven.atlas.spi.neo4j.effective.traverse.TraversalUtils.acceptedInView;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectRelationship;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.effective.GraphView;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

public abstract class AbstractAtlasCollector<T>
    implements AtlasCollector<T>
{

    protected final Logger logger = new Logger( getClass() );

    protected boolean logEnabled = false;

    protected Direction direction = Direction.OUTGOING;

    protected final Set<Node> startNodes;

    protected final Set<T> found = new HashSet<T>();

    protected final Set<Long> seen = new HashSet<Long>();

    protected final boolean checkExistence;

    protected GraphView view;

    protected AbstractAtlasCollector( final Node start, final GraphView view, final boolean checkExistence )
    {
        this( Collections.singleton( start ), view, checkExistence );
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence )
    {
        this.startNodes = startNodes;
        this.view = view;
        this.checkExistence = checkExistence;
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final boolean checkExistence,
                                      final Direction direction )
    {
        this( startNodes, view, checkExistence );
        this.view = view;
        this.direction = direction;
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public final Iterable<Relationship> expand( final Path path, final BranchState state )
    {
        if ( checkExistence && !found.isEmpty() )
        {
            log( "Only checking for existence, and already found one. Rejecting: %s", path );
            return Collections.emptySet();
        }

        if ( !startNodes.isEmpty() && !startNodes.contains( path.startNode() ) )
        {
            log( "Rejecting path; it does not start with one of our roots:\n\t%s", path );
            return Collections.emptySet();
        }

        final Relationship lastRelationship = path.lastRelationship();
        if ( lastRelationship != null )
        {
            // NOTE: Have to use relationshipId, because multiple relationships may exist between any two GAVs.
            // Most common is managed and unmanaged flavors of the same basic relationship (eg. dependencies).
            final Long endId = lastRelationship.getId();

            if ( seen.contains( endId ) )
            {
                log( "Rejecting path; already seen it:\n\t%s", path );
                return Collections.emptySet();
            }

            seen.add( endId );
        }

        if ( returnChildren( path ) )
        {
            final ProjectRelationship<?> rel = toProjectRelationship( path.lastRelationship() );
            log( "Implementation says return the children of: %s (lastRel=%s)",
                 path.endNode()
                     .hasProperty( GAV ) ? path.endNode()
                                               .getProperty( GAV ) : "Unknown", rel );

            final Iterable<Relationship> relationships = path.endNode()
                                                             .getRelationships( direction );
            //            for ( final Relationship r : relationships )
            //            {
            //                log( "Expand included relationship: %s", toProjectRelationship( r ) );
            //            }

            return relationships;
        }

        return Collections.emptySet();
    }

    protected abstract boolean returnChildren( Path path );

    protected boolean accept( final Path path )
    {
        return acceptedInView( path, view );
    }

    @Override
    public final Evaluation evaluate( final Path path )
    {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    protected void log( final String format, final Object... params )
    {
        if ( logEnabled )
        {
            logger.info( format, params );
        }
    }

}
