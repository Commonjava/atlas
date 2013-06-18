package org.commonjava.maven.atlas.spi.neo4j.effective.traverse;

import static org.apache.maven.graph.effective.util.RelationshipUtils.POM_ROOT_URI;
import static org.apache.maven.graph.effective.util.RelationshipUtils.UNKNOWN_SOURCE_URI;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.DESELECTED_FOR;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.POM_LOCATION_URI;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.SELECTED_FOR;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.SOURCE_URI;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getURIListProperty;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getURIProperty;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.idListingContains;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.isSelectionOnly;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectRelationship;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.session.EGraphSession;
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

    protected EGraphSession session;

    protected AbstractAtlasCollector( final Node start, final EGraphSession session,
                                      final ProjectRelationshipFilter filter, final boolean checkExistence )
    {
        this( Collections.singleton( start ), session, filter, checkExistence );
        this.session = session;
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final EGraphSession session,
                                      final ProjectRelationshipFilter filter, final boolean checkExistence )
    {
        this.startNodes = startNodes;
        this.session = session;
        this.filter = filter;
        this.checkExistence = checkExistence;
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final EGraphSession session,
                                      final ProjectRelationshipFilter filter, final boolean checkExistence,
                                      final Direction direction )
    {
        this( startNodes, session, filter, checkExistence );
        this.session = session;
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
            if ( session != null )
            {
                final long sessionId = Long.parseLong( session.getId() );
                if ( idListingContains( DESELECTED_FOR, r, sessionId ) )
                {
                    log( "Found relationship in path that was deselected: %s", r );
                    return false;
                }

                if ( isSelectionOnly( r ) && !idListingContains( SELECTED_FOR, r, sessionId ) )
                {
                    log( "Found relationship in path that was not selected and is marked as selection-only: %s", r );
                    return false;
                }

                final Set<URI> sources = session.getActiveSources();
                if ( sources != null && !sources.isEmpty() )
                {
                    final List<URI> s = getURIListProperty( SOURCE_URI, r, UNKNOWN_SOURCE_URI );
                    boolean found = false;
                    for ( final URI uri : s )
                    {
                        if ( sources.contains( uri ) )
                        {
                            found = true;
                            break;
                        }
                    }

                    if ( !found )
                    {
                        log( "Found relationship in path with de-selected source-repository URI: %s", r );
                        return false;
                    }
                }

                final Set<URI> pomLocations = session.getActivePomLocations();
                if ( pomLocations != null && !pomLocations.isEmpty() )
                {
                    final URI pomLocation = getURIProperty( POM_LOCATION_URI, r, POM_ROOT_URI );
                    if ( !pomLocations.contains( pomLocation ) )
                    {
                        log( "Found relationship in path with de-selected pom-location URI: %s", r );
                        return false;
                    }
                }
            }

            if ( f != null )
            {
                final ProjectRelationship<?> rel = toProjectRelationship( r );
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
