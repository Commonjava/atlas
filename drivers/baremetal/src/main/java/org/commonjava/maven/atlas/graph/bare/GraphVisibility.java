package org.commonjava.maven.atlas.graph.bare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphVisibility
{

    private final GraphView view;

    private final Set<ProjectRelationship<?>> includedRelationships = new HashSet<>();

    private final Set<ProjectVersionRef> includedProjects = new HashSet<>();

    private final Map<String, Set<ProjectRelationshipFilter>> pathFilters = new HashMap<>();

    public GraphVisibility( final GraphView view )
    {
        this.view = view;
        if ( view.getRoots() != null )
        {
            includedProjects.addAll( view.getRoots() );
        }
    }

    public boolean addPathExtensionIfVisible( final ProjectRelationship<?> last, final Set<AtlasPath> knownParents )
    {
        // if the path is rooted outside this view's roots, reject it immediately
        final Set<ProjectVersionRef> roots = view.getRoots();
        if ( roots != null && !roots.contains( path.getStartGAV() ) )
        {
            return false;
        }

        final List<ProjectRelationship<?>> previousParts = path.getPreviousParts();
        ProjectRelationship<?> previous = null;
        if ( previousParts != null && !previousParts.isEmpty() )
        {
            previous = previousParts.get( previousParts.size() - 1 );
        }

        if ( previous != null )
        {
            if ( !includedRelationships.contains( previous ) )
            {
                return false;
            }

            final Set<ProjectRelationshipFilter> filters = pathFilters.get( previous );
            Set<ProjectRelationshipFilter> lastFilters = null;
            if ( filters != null && !filters.isEmpty() )
            {
                for ( final ProjectRelationshipFilter filter : filters )
                {
                    if ( filter.accept( last ) )
                    {
                        final ProjectRelationshipFilter lastFilter = filter.getChildFilter( last );

                        if ( lastFilter != null )
                        {
                            if ( lastFilters == null )
                            {
                                lastFilters = new HashSet<>();
                                pathFilters.put( last, lastFilters );
                            }

                            lastFilters.add( lastFilter );
                        }
                    }
                }

                if ( lastFilters != null && !lastFilters.isEmpty() )
                {
                    includedRelationships.add( last );
                    includedProjects.add( last.getDeclaring() );
                    includedProjects.add( last.getTarget()
                                              .asProjectVersionRef() );

                    return true;
                }
            }
        }
    }

}
