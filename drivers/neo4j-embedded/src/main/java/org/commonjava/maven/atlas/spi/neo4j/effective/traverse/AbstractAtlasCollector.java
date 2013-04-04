package org.commonjava.maven.atlas.spi.neo4j.effective.traverse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
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

    protected final ProjectRelationshipFilter filter;

    protected final boolean checkExistence;

    protected AbstractAtlasCollector( final Node start, final ProjectRelationshipFilter filter,
                                      final boolean checkExistence )
    {
        this( Collections.singleton( start ), filter, checkExistence );
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final ProjectRelationshipFilter filter,
                                      final boolean checkExistence )
    {
        this.startNodes = startNodes;
        this.filter = filter;
        this.checkExistence = checkExistence;
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final ProjectRelationshipFilter filter,
                                      final boolean checkExistence, final Direction direction )
    {
        this( startNodes, filter, checkExistence );
        this.direction = direction;
    }

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

        final Long endId = path.endNode()
                               .getId();

        if ( seen.contains( endId ) )
        {
            log( "Rejecting path; already seen it:\n\t%s", path );
            return Collections.emptySet();
        }

        seen.add( endId );

        if ( returnChildren( path ) )
        {
            log( "Implementation says return the children of: %s", path.endNode() );
            return path.endNode()
                       .getRelationships( direction );
        }

        return Collections.emptySet();
    }

    protected abstract boolean returnChildren( Path path );

    protected boolean accept( final Path path )
    {
        ProjectRelationshipFilter f = filter;
        for ( final Relationship r : path.relationships() )
        {
            log( "Checking relationship for acceptance: %s", r );
            if ( !startNodes.isEmpty() && Conversions.idListingContains( Conversions.DESELECTED_FOR, r, startNodes ) )
            {
                log( "Found relationship in path that was deselected: %s", r );
                return false;
            }

            if ( f != null )
            {
                final ProjectRelationship<?> rel = Conversions.toProjectRelationship( r );
                if ( !f.accept( rel ) )
                {
                    log( "Filter rejected relationship: %s", rel );
                    return false;
                }

                f = f.getChildFilter( rel );
            }
        }

        log( "Path accepted: %s", path );
        return true;
    }

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
