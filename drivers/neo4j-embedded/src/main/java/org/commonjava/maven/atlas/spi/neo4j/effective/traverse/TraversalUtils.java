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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.GraphView;
import org.apache.maven.graph.effective.filter.AbstractAggregatingFilter;
import org.apache.maven.graph.effective.filter.AbstractTypedFilter;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.workspace.GraphWorkspace;
import org.apache.maven.graph.effective.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.spi.neo4j.effective.GraphRelType;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public final class TraversalUtils
{

    private static final Logger logger = new Logger( TraversalUtils.class );

    private TraversalUtils()
    {
    }

    public static boolean acceptedInView( final Path path, final GraphView view )
    {
        ProjectRelationshipFilter f = view.getFilter();
        final GraphWorkspace workspace = view.getWorkspace();

        for ( final Relationship r : path.relationships() )
        {
            if ( !accepted( r, f, workspace ) )
            {
                return false;
            }

            if ( f != null )
            {
                final ProjectRelationship<?> rel = toProjectRelationship( r );
                f = f.getChildFilter( rel );
            }
        }

        logger.debug( "Path accepted: %s", path );
        return true;
    }

    public static boolean acceptedInView( final Relationship r, final GraphView view )
    {
        return accepted( r, view.getFilter(), view.getWorkspace() );
    }

    private static boolean accepted( final Relationship r, final ProjectRelationshipFilter f,
                                     final GraphWorkspace workspace )
    {
        logger.debug( "Checking relationship for acceptance: %s", r );
        if ( workspace != null )
        {
            final long workspaceId = Long.parseLong( workspace.getId() );
            if ( idListingContains( DESELECTED_FOR, r, workspaceId ) )
            {
                logger.debug( "Found relationship in path that was deselected: %s", r );
                return false;
            }

            if ( isSelectionOnly( r ) && !idListingContains( SELECTED_FOR, r, workspaceId ) )
            {
                logger.debug( "Found relationship in path that was not selected and is marked as selection-only: %s", r );
                return false;
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
                    logger.debug( "Found relationship in path with de-selected source-repository URI: %s", r );
                    return false;
                }
            }

            final Set<URI> pomLocations = workspace.getActivePomLocations();
            if ( pomLocations != null && !pomLocations.isEmpty() )
            {
                final URI pomLocation = getURIProperty( POM_LOCATION_URI, r, POM_ROOT_URI );
                if ( !pomLocations.contains( pomLocation ) )
                {
                    logger.debug( "Found relationship in path with de-selected pom-location URI: %s", r );
                    return false;
                }
            }
        }

        if ( f != null )
        {
            final ProjectRelationship<?> rel = toProjectRelationship( r );
            if ( !f.accept( rel ) )
            {
                logger.debug( "Filter rejected relationship: %s", rel );
                return false;
            }
        }

        return true;
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
            final List<? extends ProjectRelationshipFilter> filters =
                ( (AbstractAggregatingFilter) filter ).getFilters();

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
