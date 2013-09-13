package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.POM_LOCATION_URI;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.SOURCE_URI;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getURIListProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.getURIProperty;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isDeselected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isSelected;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.isSelectionOnly;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.POM_ROOT_URI;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.UNKNOWN_SOURCE_URI;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.AbstractAggregatingFilter;
import org.commonjava.maven.atlas.graph.filter.AbstractTypedFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public final class TraversalUtils
{

    private static final Logger logger = new Logger( TraversalUtils.class );

    private TraversalUtils()
    {
    }

    public static boolean acceptedInView( final Path path, final GraphView view, final Node wsNode )
    {
        ProjectRelationshipFilter f = view.getFilter();
        final GraphWorkspace ws = view.getWorkspace();

        for ( final Relationship r : path.relationships() )
        {
            if ( !accepted( r, f, ws, wsNode ) )
            {
                return false;
            }

            if ( f != null )
            {
                final ProjectRelationship<?> rel = toProjectRelationship( r );
                f = f.getChildFilter( rel );
            }
        }

        debug( "ACCEPT: Path: %s", path );
        return true;
    }

    public static boolean acceptedInView( final Relationship r, final GraphView view, final Node wsNode )
    {
        return accepted( r, view.getFilter(), view.getWorkspace(), wsNode );
    }

    public static boolean accepted( final Relationship r, final ProjectRelationshipFilter f, final GraphWorkspace workspace, final Node wsNode )
    {
        final ProjectRelationship<?> rel = toProjectRelationship( r );

        debug( "Checking relationship for acceptance: %s (%s)", r, rel );

        if ( wsNode != null )
        {
            final Node endNode = r.getEndNode();
            if ( r != null && isDeselected( endNode, wsNode ) )
            {
                debug( "REJECTED: Found relationship in path that was deselected: %s", r );
                return false;
            }

            if ( r != null && isSelectionOnly( r ) && !isSelected( endNode, wsNode ) )
            {
                debug( "REJECTED: Found relationship in path that was not selected and is marked as selection-only: %s", r );
                return false;
            }
        }

        final Set<URI> sources = workspace.getActiveSources();
        if ( sources != null && !sources.isEmpty() )
        {
            final List<URI> s = getURIListProperty( SOURCE_URI, r, UNKNOWN_SOURCE_URI );
            boolean found = false;
            for ( final URI uri : s )
            {
                if ( sources == GraphWorkspaceConfiguration.DEFAULT_SOURCES || sources.contains( uri ) )
                {
                    found = true;
                    break;
                }
            }

            if ( !found )
            {
                debug( "REJECTED: Found relationship in path with de-selected source-repository URI: %s", r );
                return false;
            }
        }

        final Set<URI> pomLocations = workspace.getActivePomLocations();
        if ( pomLocations != null && !pomLocations.isEmpty() )
        {
            final URI pomLocation = getURIProperty( POM_LOCATION_URI, r, POM_ROOT_URI );
            if ( !pomLocations.contains( pomLocation ) )
            {
                debug( "REJECTED: Found relationship in path with de-selected pom-location URI: %s", r );
                return false;
            }
        }

        if ( f != null )
        {
            if ( !f.accept( rel ) )
            {
                debug( "Filter: %s REJECTED relationship: %s (%s)", f, r, rel );
                return false;
            }
        }

        debug( "ACCEPT: %s (%s)", r, rel );
        return true;
    }

    private static void debug( final String message, final Object... params )
    {
        logger.debug( message, params );
    }

    public static Set<GraphRelType> getGraphRelTypes( final ProjectRelationshipFilter filter )
    {
        if ( filter == null )
        {
            return GraphRelType.atlasRelationshipTypes();
        }

        final Set<GraphRelType> result = new HashSet<GraphRelType>();

        if ( filter instanceof AbstractTypedFilter )
        {
            final AbstractTypedFilter typedFilter = (AbstractTypedFilter) filter;
            final Set<RelationshipType> types = typedFilter.getRelationshipTypes();
            for ( final RelationshipType rt : types )
            {
                if ( typedFilter.isManagedInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, true );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }

                if ( typedFilter.isConcreteInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, false );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }
            }

            final Set<RelationshipType> dTypes = typedFilter.getDescendantRelationshipTypes();
            for ( final RelationshipType rt : dTypes )
            {
                if ( typedFilter.isManagedInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, true );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }

                if ( typedFilter.isConcreteInfoIncluded() )
                {
                    final GraphRelType grt = GraphRelType.map( rt, false );
                    if ( grt != null )
                    {
                        result.add( grt );
                    }
                }
            }
        }
        else if ( filter instanceof AbstractAggregatingFilter )
        {
            final List<? extends ProjectRelationshipFilter> filters = ( (AbstractAggregatingFilter) filter ).getFilters();

            for ( final ProjectRelationshipFilter f : filters )
            {
                result.addAll( getGraphRelTypes( f ) );
            }
        }
        else
        {
            result.addAll( GraphRelType.atlasRelationshipTypes() );
        }

        return result;
    }

}
