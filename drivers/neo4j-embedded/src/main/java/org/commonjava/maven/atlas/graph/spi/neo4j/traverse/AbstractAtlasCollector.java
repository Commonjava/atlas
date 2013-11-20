package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isDeselected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectVersionRef;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.accepted;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
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

    protected final Node wsNode;

    protected GraphView view;

    protected Map<Long, ProjectRelationshipFilter> relationshipFilters = new HashMap<Long, ProjectRelationshipFilter>();

    protected AbstractAtlasCollector( final Node start, final GraphView view, final Node wsNode, final boolean checkExistence )
    {
        this( Collections.singleton( start ), view, wsNode, checkExistence );
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final Node wsNode, final boolean checkExistence )
    {
        this.startNodes = startNodes;
        this.view = view;
        this.wsNode = wsNode;
        this.checkExistence = checkExistence;
    }

    protected AbstractAtlasCollector( final Set<Node> startNodes, final GraphView view, final Node wsNode, final boolean checkExistence,
                                      final Direction direction )
    {
        this( startNodes, view, wsNode, checkExistence );
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
            final ProjectRelationship<?> rel = toProjectRelationship( lastRelationship );

            ProjectRelationshipFilter nextFilter = null;
            if ( lastRelationship != null )
            {
                final Long endId = lastRelationship.getId();
                ProjectRelationshipFilter lastFilter = relationshipFilters.remove( endId );
                if ( lastFilter == null )
                {
                    lastFilter = view.getFilter();
                }

                if ( lastFilter != null )
                {
                    nextFilter = lastFilter.getChildFilter( rel );
                }
            }

            log( "Implementation says return the children of: %s\n  lastRel=%s\n  nextFilter=%s\n\n",
                 path.endNode()
                     .hasProperty( GAV ) ? path.endNode()
                                               .getProperty( GAV ) : "Unknown", rel, nextFilter );

            final Set<Relationship> nextRelationships = new HashSet<Relationship>();
            final Iterable<Relationship> relationships = path.endNode()
                                                             .getRelationships( direction );
            for ( final Relationship r : relationships )
            {
                nextRelationships.add( r );

                if ( nextFilter != null )
                {
                    relationshipFilters.put( r.getId(), nextFilter );
                    log( "+= %s [%s]", logwrapper( r ), nextFilter );
                }
                else
                {
                    log( "+= %s [global: %s]", logwrapper( r ), view.getFilter() );
                }
            }

            return nextRelationships;
        }

        return Collections.emptySet();
    }

    protected abstract boolean returnChildren( Path path );

    protected boolean accept( final Path path )
    {
        final Relationship r = path.lastRelationship();
        if ( r == null )
        {
            return !isDeselected( path.endNode(), wsNode );
        }

        ProjectRelationshipFilter filter = relationshipFilters.get( r.getId() );
        if ( filter == null )
        {
            filter = view.getFilter();
        }

        final boolean accept = accepted( r, filter, view.getWorkspace(), wsNode );

        if ( logEnabled )
        {
            final Set<ProjectVersionRef> gavs = new HashSet<ProjectVersionRef>( startNodes.size() );
            for ( final Node node : startNodes )
            {
                gavs.add( toProjectVersionRef( node ) );
            }

            log( "Checking acceptance: %s [roots: %s, filter: %s]...%s", logwrapper( r ), gavs, filter, accept );
        }

        return accept;
    }

    private Object logwrapper( final Relationship r )
    {
        return new Object()
        {
            @Override
            public String toString()
            {
                return String.valueOf( toProjectRelationship( r ) );
            }
        };
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
