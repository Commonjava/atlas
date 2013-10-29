/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.spi.jung;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.POM_ROOT_URI;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.UNKNOWN_SOURCE_URI;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.AbstractProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipComparator;
import org.commonjava.maven.atlas.graph.rel.RelationshipPathComparator;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.FilteringTraversal;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.util.logging.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class JungEGraphDriver
    implements GraphDatabaseDriver
{
    private static final String PATHS = "paths";

    private final Logger logger = new Logger( getClass() );

    private final DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph = new DirectedSparseMultigraph<>();

    private final Map<ProjectRef, Set<ProjectVersionRef>> byGA = new HashMap<>();

    private transient Map<ProjectRef, ProjectVersionRef> selectedForAll = new HashMap<>();

    private final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<>();

    private final Set<ProjectVersionRef> variableSubgraphs = new HashSet<>();

    private final Map<ProjectVersionRef, ProjectVersionRef> selected = new HashMap<>();

    private final Map<ProjectVersionRef, Map<String, String>> metadata = new HashMap<>();

    private final Map<String, Set<ProjectVersionRef>> metadataOwners = new HashMap<>();

    private transient Map<GraphView, Map<String, Object>> caches = new WeakHashMap<>();

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final GraphView view, final ProjectVersionRef ref )
    {
        final Map<ProjectVersionRef, PathState> pathStates = getPathStates( view );

        findPathsToProjects( getRoots( view, false ), view.getFilter(), view, false, pathStates, ref );

        final PathState state = pathStates.get( ref );
        if ( state == null )
        {
            return null;
        }

        final Collection<ProjectRelationshipFilter> filters = state.getVisiblePathFilters();
        final OrFilter filter = filters == null || filters.isEmpty() ? null : new OrFilter( filters );

        final Collection<ProjectRelationship<?>> edges = new HashSet<>( graph.getOutEdges( ref.asProjectVersionRef() ) );
        for ( final Iterator<ProjectRelationship<?>> iterator = edges.iterator(); iterator.hasNext(); )
        {
            final ProjectRelationship<?> rel = iterator.next();
            if ( filter != null && !filter.accept( rel ) )
            {
                iterator.remove();
            }
        }

        return imposeSelections( view, edges, false );
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final GraphView view, final ProjectVersionRef ref )
    {
        final Collection<ProjectRelationship<?>> edges = new HashSet<>( graph.getInEdges( ref.asProjectVersionRef() ) );

        final Map<ProjectVersionRef, PathState> pathStates = getPathStates( view );

        findPathsToRelationships( getRoots( view, false ), view.getFilter(), view, false, pathStates,
                                  edges.toArray( new ProjectRelationship<?>[edges.size()] ) );

        final PathState state = pathStates.get( ref );
        return state.getVisibleTargetingRelationships();
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        final Set<ProjectVersionRef> roots = getRoots( view, false );
        if ( roots != null && !roots.isEmpty() )
        {
            final FilteringTraversal traversal = new FilteringTraversal( view.getFilter(), true );

            // FIXME: if the view doesn't have any roots, this will fail.
            dfsTraverse( view, traversal, 0, view.getRoots()
                                                 .toArray( new ProjectVersionRef[view.getRoots()
                                                                                     .size()] ) );

            return traversal.getCapturedRelationships();
        }

        return imposeSelections( view, graph.getEdges(), true );
    }

    private Collection<ProjectRelationship<?>> imposeSelections( final GraphView view, final Collection<ProjectRelationship<?>> edges,
                                                                 final boolean doFilter )
    {
        if ( edges == null || edges.isEmpty() )
        {
            return edges;
        }

        if ( view == null || ( view.getWorkspace() == null && ( !doFilter || view.getFilter() == null ) ) )
        {
            // no selections here...
            return edges;
        }

        final GraphWorkspace workspace = view.getWorkspace();
        final ProjectRelationshipFilter filter = doFilter ? view.getFilter() : null;

        final List<ProjectRelationship<?>> result = new ArrayList<ProjectRelationship<?>>( edges.size() );
        for ( final ProjectRelationship<?> edge : edges )
        {
            if ( filter != null && !filter.accept( edge ) )
            {
                continue;
            }

            final ProjectVersionRef target = edge.getTarget()
                                                 .asProjectVersionRef();
            final ProjectVersionRef selected = getSelectedVersion( target );
            final Set<URI> sources = workspace.getActiveSources();
            if ( sources != null && !sources.isEmpty() )
            {
                Set<URI> s = edge.getSources();
                if ( s == null )
                {
                    s = Collections.singleton( UNKNOWN_SOURCE_URI );
                }

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
                    //                    log( "Found relationship in path with de-selected source-repository URI: %s", edge );
                    continue;
                }
            }

            final Set<URI> pomLocations = workspace.getActivePomLocations();
            if ( pomLocations != null && !pomLocations.isEmpty() )
            {
                URI pomLocation = edge.getPomLocation();
                if ( pomLocation == null )
                {
                    pomLocation = POM_ROOT_URI;
                }
                if ( !pomLocations.contains( pomLocation ) )
                {
                    //                    log( "Found relationship in path with de-selected pom-location URI: %s", edge );
                    continue;
                }
            }

            if ( selected != null )
            {
                // FIXME: Fix the api to allow relocations!
                result.add( edge.selectTarget( (SingleVersion) selected.getVersionSpec() ) );
            }
            else
            {
                result.add( edge );
            }
        }

        return result;
    }

    private ProjectVersionRef getSelectedVersion( final ProjectVersionRef ref )
    {
        ProjectVersionRef result = selected.get( ref.asProjectVersionRef() );

        if ( result == null )
        {
            result = selectedForAll.get( ref.asProjectRef() );
        }

        return result;
    }

    @Override
    public Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rels )
    {
        final Set<ProjectRelationship<?>> skipped = new HashSet<ProjectRelationship<?>>();
        final Set<ProjectVersionRef> targets = new HashSet<>( rels.length );
        for ( final ProjectRelationship<?> rel : rels )
        {
            final ProjectVersionRef target = rel.getTarget()
                                                .asProjectVersionRef();

            targets.add( target );

            try
            {
                if ( !target.getVersionSpec()
                            .isSingle() )
                {
                    logger.info( "Adding variable target: %s", target );
                    variableSubgraphs.add( target );
                }
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                logger.error( "Failed to determine variable-version status of: %s. Reason: %s", e, target, e.getMessage() );
                skipped.add( rel );
                continue;
            }

            if ( !graph.containsVertex( rel.getDeclaring() ) )
            {
                logger.info( "Adding node: %s", rel.getDeclaring() );
                graph.addVertex( rel.getDeclaring() );
                addGA( rel.getDeclaring() );
            }

            if ( !graph.containsVertex( target ) )
            {
                logger.info( "Adding incomplete target: %s", target );
                incompleteSubgraphs.add( target );
            }

            if ( !graph.containsVertex( target ) )
            {
                logger.info( "Adding node: %s", target );
                graph.addVertex( target.asProjectVersionRef() );
                addGA( target );
            }

            final List<ProjectRelationship<?>> edges = new ArrayList<ProjectRelationship<?>>( graph.findEdgeSet( rel.getDeclaring(), target ) );
            if ( !edges.contains( rel ) )
            {
                logger.info( "Adding edge: %s -> %s", rel.getDeclaring(), target );
                graph.addEdge( rel, rel.getDeclaring(), target.asProjectVersionRef() );
            }
            else
            {
                final int idx = edges.indexOf( rel );
                final ProjectRelationship<?> existing = edges.get( idx );

                logger.info( "Adding sources: %s to existing edge: %s", rel.getSources(), existing );

                existing.addSources( rel.getSources() );
            }

            logger.info( "removing from incomplete status: %s", rel.getDeclaring() );
            incompleteSubgraphs.remove( rel.getDeclaring() );
        }

        final CycleDetectionTraversal traversal = new CycleDetectionTraversal();
        dfsTraverse( GraphView.GLOBAL, traversal, 0, targets.toArray( new ProjectVersionRef[targets.size()] ) );

        logger.info( "Detecting cycles..." );

        final Collection<EProjectCycle> cycles = traversal.getCycles();

        if ( !cycles.isEmpty() )
        {
            logger.info( "%d CYCLES found!\n  %s", cycles.size(), join( cycles, "\n  " ) );

            this.cycles.addAll( cycles );

            for ( final ProjectRelationship<?> rel : traversal.getCycleInjectors() )
            {
                skipped.add( rel );
                graph.removeEdge( rel );
            }
        }

        clearAddedTargetStates( rels, skipped );

        return skipped;
    }

    private void clearAddedTargetStates( final ProjectRelationship<?>[] rels, final Set<ProjectRelationship<?>> skipped )
    {
        for ( final GraphView view : caches.keySet() )
        {
            final Map<ProjectVersionRef, PathState> pathStates = getPathStates( view, false );
            if ( pathStates != null )
            {
                for ( final ProjectRelationship<?> rel : rels )
                {
                    if ( skipped.contains( rel ) )
                    {
                        continue;
                    }

                    pathStates.remove( rel.getTarget()
                                          .asProjectVersionRef() );
                }
            }
        }
    }

    private boolean addGA( final ProjectVersionRef ref )
    {
        final ProjectRef pr = ref.asProjectRef();
        Set<ProjectVersionRef> refs = byGA.get( pr );
        if ( refs == null )
        {
            refs = new HashSet<ProjectVersionRef>();
            byGA.put( pr, refs );
        }

        return refs.add( ref );
    }

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final GraphView view, final ProjectVersionRef... refs )
    {
        if ( view.getRoots() == null || view.getRoots()
                                            .isEmpty() )
        {
            logger.warn( "Cannot retrieve paths targeting %s. No roots specified for this project network!", join( refs, ", " ) );
            return null;
        }

        final Map<ProjectVersionRef, PathState> pathStates = getPathStates( view );
        findPathsToProjects( view.getRoots(), view.getFilter(), view, false, pathStates, refs );

        final Set<List<ProjectRelationship<?>>> result = new HashSet<>();
        for ( final ProjectVersionRef ref : refs )
        {
            final PathState state = pathStates.get( ref );
            if ( state != null )
            {
                result.addAll( state.getVisiblePaths() );
            }
        }

        return result;
    }

    private void findPathsToProjects( final Set<ProjectVersionRef> roots, final ProjectRelationshipFilter filter, final GraphView view,
                                      final boolean detectOnly, final Map<ProjectVersionRef, PathState> pathStates,
                                      final Collection<ProjectVersionRef> refs )
    {
        boolean reTraverse = false;
        for ( final ProjectVersionRef ref : refs )
        {
            if ( !pathStates.containsKey( ref ) )
            {
                reTraverse = true;
                break;
            }
        }

        if ( reTraverse )
        {
            final PathMappingTraversal traversal = new PathMappingTraversal( filter, pathStates );

            if ( roots == null || roots.isEmpty() )
            {
                logger.warn( "Cannot retrieve paths targeting %s. No roots specified for this project network!", join( refs, ", " ) );
                return;
            }

            dfsTraverse( view, traversal, 0, roots.toArray( new ProjectVersionRef[roots.size()] ) );
        }
    }

    private void findPathsToProjects( final Set<ProjectVersionRef> roots, final ProjectRelationshipFilter filter, final GraphView view,
                                      final boolean detectOnly, final Map<ProjectVersionRef, PathState> pathStates, final ProjectVersionRef... refs )
    {
        boolean reTraverse = false;
        for ( final ProjectVersionRef ref : refs )
        {
            if ( !pathStates.containsKey( ref ) )
            {
                reTraverse = true;
                break;
            }
        }

        if ( reTraverse )
        {
            final FilteringTraversal traversal = new FilteringTraversal( filter, true );

            if ( roots == null || roots.isEmpty() )
            {
                logger.warn( "Cannot retrieve paths targeting %s. No roots specified for this project network!", join( refs, ", " ) );
                return;
            }

            dfsTraverse( view, traversal, 0, roots.toArray( new ProjectVersionRef[roots.size()] ) );
        }
    }

    private void findPathsToRelationships( final Set<ProjectVersionRef> roots, final ProjectRelationshipFilter filter, final GraphView view,
                                           final boolean detectOnly, final Map<ProjectVersionRef, PathState> pathStates,
                                           final ProjectRelationship<?>... rels )
    {
        boolean reTraverse = false;
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( !pathStates.containsKey( rel.getTarget()
                                             .asProjectVersionRef() ) )
            {
                reTraverse = true;
                break;
            }
        }

        if ( reTraverse )
        {
            final FilteringTraversal traversal = new FilteringTraversal( filter, true );

            if ( roots == null || roots.isEmpty() )
            {
                logger.warn( "Cannot retrieve paths targeting %s. No roots specified for this project network!", join( rels, ", " ) );
                return;
            }

            dfsTraverse( view, traversal, 0, roots.toArray( new ProjectVersionRef[roots.size()] ) );
        }
    }

    @Override
    public boolean introducesCycle( final GraphView view, final ProjectRelationship<?> rel )
    {
        final SingleCycleDetectionTraversal traversal = new SingleCycleDetectionTraversal( rel );

        dfsTraverse( view, traversal, 0, rel.getTarget()
                                            .asProjectVersionRef() );

        return !traversal.getCycles()
                         .isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        return findVisibleProjects( getRoots( view, false ), view.getFilter(), view, graph.getVertices() );
    }

    @Override
    public void traverse( final GraphView view, final ProjectNetTraversal traversal, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        ProjectVersionRef[] start;
        if ( roots.length > 0 )
        {
            start = roots;
        }
        else if ( view.getRoots() != null && !view.getRoots()
                                                  .isEmpty() )
        {
            start = view.getRoots()
                        .toArray( new ProjectVersionRef[view.getRoots()
                                                            .size()] );
        }
        else
        {
            logger.error( "Cannot traverse; no roots specified!" );
            return;
        }

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            traversal.startTraverse( i );

            switch ( traversal.getType( i ) )
            {
                case breadth_first:
                {
                    bfsTraverse( view, traversal, i, start );
                    break;
                }
                case depth_first:
                {
                    dfsTraverse( view, traversal, i, start );
                    break;
                }
            }

            traversal.endTraverse( i );
        }
    }

    // TODO: Implement without recursion.
    private void dfsTraverse( final GraphView view, final ProjectNetTraversal traversal, final int pass, final ProjectVersionRef... roots )
    {
        for ( final ProjectVersionRef root : roots )
        {
            dfsIterate( view, root, traversal, new LinkedList<ProjectRelationship<?>>(), pass );
        }
    }

    private void dfsIterate( final GraphView view, final ProjectVersionRef node, final ProjectNetTraversal traversal,
                             final LinkedList<ProjectRelationship<?>> path, final int pass )
    {
        final List<ProjectRelationship<?>> edges = getSortedOutEdges( view, node );
        if ( edges != null )
        {
            for ( final ProjectRelationship<?> edge : edges )
            {
                if ( traversal.traverseEdge( edge, path, pass ) )
                {

                    if ( !( edge instanceof ParentRelationship ) || !( (ParentRelationship) edge ).isTerminus() )
                    {
                        path.addLast( edge );

                        final ProjectVersionRef target = edge.getTarget()
                                                             .asProjectVersionRef();

                        // FIXME: Are there cases where a traversal needs to see cycles??
                        boolean cycle = false;
                        for ( final ProjectRelationship<?> item : path )
                        {
                            if ( item.getDeclaring()
                                     .equals( target ) )
                            {
                                cycle = true;
                                break;
                            }
                        }

                        if ( !cycle )
                        {
                            dfsIterate( view, target, traversal, path, pass );
                        }

                        path.removeLast();
                    }

                    traversal.edgeTraversed( edge, path, pass );
                    if ( traversal.isStopped() )
                    {
                        return;
                    }
                }

            }
        }
    }

    // TODO: Implement without recursion.
    private void bfsTraverse( final GraphView view, final ProjectNetTraversal traversal, final int pass, final ProjectVersionRef... roots )
    {
        final List<List<ProjectRelationship<?>>> starts = new ArrayList<>();
        for ( final ProjectVersionRef root : roots )
        {
            final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
            path.add( new SelfEdge( root ) );
            starts.add( path );
        }

        bfsIterate( view, starts, traversal, pass );
    }

    private void bfsIterate( final GraphView view, final List<List<ProjectRelationship<?>>> thisLayer, final ProjectNetTraversal traversal,
                             final int pass )
    {
        final List<List<ProjectRelationship<?>>> nextLayer = new ArrayList<List<ProjectRelationship<?>>>();

        for ( final List<ProjectRelationship<?>> path : thisLayer )
        {
            if ( path.isEmpty() )
            {
                continue;
            }

            final ProjectVersionRef node = path.get( path.size() - 1 )
                                               .getTarget()
                                               .asProjectVersionRef();

            if ( !path.isEmpty() && ( path.get( 0 ) instanceof SelfEdge ) )
            {
                path.remove( 0 );
            }

            final List<ProjectRelationship<?>> edges = getSortedOutEdges( view, node );
            if ( edges != null )
            {
                for ( final ProjectRelationship<?> edge : edges )
                {
                    // call traverseEdge no matter what, to allow traversal to "see" all relationships.
                    if ( /*( edge instanceof SelfEdge ) ||*/traversal.traverseEdge( edge, path, pass ) )
                    {
                        // Don't account for terminal parent relationship.
                        if ( !( edge instanceof ParentRelationship ) || !( (ParentRelationship) edge ).isTerminus() )
                        {
                            final List<ProjectRelationship<?>> nextPath = new ArrayList<ProjectRelationship<?>>( path );

                            // FIXME: How do we avoid cycle traversal here??
                            nextPath.add( edge );

                            nextLayer.add( nextPath );
                        }

                        traversal.edgeTraversed( edge, path, pass );
                        if ( traversal.isStopped() )
                        {
                            return;
                        }
                    }
                }
            }
        }

        if ( !nextLayer.isEmpty() )
        {
            Collections.sort( nextLayer, new RelationshipPathComparator() );
            bfsIterate( view, nextLayer, traversal, pass );
        }
    }

    private List<ProjectRelationship<?>> getSortedOutEdges( final GraphView view, final ProjectVersionRef node )
    {
        Collection<ProjectRelationship<?>> unsorted = graph.getOutEdges( node.asProjectVersionRef() );
        if ( unsorted == null )
        {
            return null;
        }

        unsorted = new ArrayList<ProjectRelationship<?>>( unsorted );

        RelationshipUtils.filterTerminalParents( unsorted );

        final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( imposeSelections( view, unsorted, false ) );
        Collections.sort( sorted, new RelationshipComparator() );

        return sorted;
    }

    private static final class SelfEdge
        extends AbstractProjectRelationship<ProjectVersionRef>
    {

        private static final long serialVersionUID = 1L;

        SelfEdge( final ProjectVersionRef ref )
        {
            super( (URI) null, null, ref.asProjectVersionRef(), ref.asProjectVersionRef(), 0 );
        }

        @Override
        public ArtifactRef getTargetArtifact()
        {
            return getTarget().asPomArtifact();
        }

        @Override
        public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
        {
            return selectDeclaring( version, false );
        }

        @Override
        public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version, final boolean force )
        {
            return new SelfEdge( getDeclaring().selectVersion( version, force ) );
        }

        @Override
        public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
        {
            return selectTarget( version, false );
        }

        @Override
        public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version, final boolean force )
        {
            return new SelfEdge( getDeclaring().selectVersion( version, force ) );
        }

    }

    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        final Set<ProjectVersionRef> singleton = new HashSet<>();
        singleton.add( ref );

        final Set<ProjectVersionRef> visibleProjects = findVisibleProjects( getRoots( view, false ), view.getFilter(), view, singleton );
        return !visibleProjects.isEmpty() && !incompleteSubgraphs.contains( ref.asProjectVersionRef() );
    }

    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        final Map<ProjectVersionRef, PathState> pathStates = getPathStates( view );

        findPathsToRelationships( getRoots( view, false ), view.getFilter(), view, true, pathStates, rel );

        final PathState state = pathStates.get( rel.getTarget()
                                                   .asProjectVersionRef() );

        return state != null && state.containsVisibleRelationship( rel );
    }

    @Override
    public void close()
        throws IOException
    {
        // NOP; stored in memory.
    }

    // FIXME: This is incredibly expensive!
    @Override
    public boolean isMissing( final GraphView view, final ProjectVersionRef project )
    {
        return getMissingProjects( view ).contains( project.asProjectVersionRef() );
    }

    // FIXME: This is incredibly expensive!
    @Override
    public boolean hasMissingProjects( final GraphView view )
    {
        return !getMissingProjects( view ).isEmpty();
    }

    // FIXME: This is incredibly expensive!
    @Override
    public Set<ProjectVersionRef> getMissingProjects( final GraphView view )
    {
        return findVisibleProjects( getRoots( view, true ), view.getFilter(), view, incompleteSubgraphs );
    }

    private Set<ProjectVersionRef> getRoots( final GraphView view, final boolean incomplete )
    {
        Set<ProjectVersionRef> roots = view.getRoots();
        if ( roots == null || roots.isEmpty() )
        {
            roots = new HashSet<>( graph.getVertices() );
            if ( !incomplete )
            {
                roots.removeAll( incompleteSubgraphs );
            }
        }

        return roots;
    }

    // FIXME: tighter synchronization
    private synchronized Set<ProjectVersionRef> findVisibleProjects( final Set<ProjectVersionRef> roots, final ProjectRelationshipFilter filter,
                                                                     final GraphView view, final Collection<ProjectVersionRef> originalRefs )
    {
        final Map<ProjectVersionRef, PathState> pathStates = getPathStates( view );
        final Set<ProjectVersionRef> found = new HashSet<>();
        final Set<ProjectVersionRef> refs = new HashSet<>( originalRefs );
        for ( final Iterator<ProjectVersionRef> iterator = refs.iterator(); iterator.hasNext(); )
        {
            final ProjectVersionRef ref = iterator.next();
            final PathState state = pathStates.get( ref );
            if ( state != null )
            {
                if ( state.isVisible() )
                {
                    found.add( ref );
                }

                iterator.remove();
            }
        }

        findPathsToProjects( roots, filter, view, true, pathStates, refs );

        for ( final ProjectVersionRef ref : refs )
        {
            final PathState state = pathStates.get( ref );
            if ( state != null )
            {
                if ( state.isVisible() )
                {
                    found.add( ref );
                }
            }
        }

        for ( final ProjectVersionRef ref : refs )
        {
            if ( roots.contains( ref ) )
            {
                found.add( ref );
            }
        }

        // logger.info( "Got %d missing projects: %s", result.size(), result );
        return found;
    }

    private Map<ProjectVersionRef, PathState> getPathStates( final GraphView view )
    {
        return getPathStates( view, true );
    }

    private Map<ProjectVersionRef, PathState> getPathStates( final GraphView view, final boolean create )
    {
        @SuppressWarnings( "unchecked" )
        Map<ProjectVersionRef, PathState> pathStates = (Map<ProjectVersionRef, PathState>) getViewCache( view, PATHS );
        if ( pathStates == null && create )
        {
            pathStates = new HashMap<>();
            setViewCache( view, PATHS, pathStates );
        }

        return pathStates;
    }

    private void setViewCache( final GraphView view, final String key, final Object value )
    {
        Map<String, Object> viewCaches = caches.get( view );
        if ( viewCaches == null )
        {
            viewCaches = new HashMap<>();
            caches.put( view, viewCaches );
        }

        viewCaches.put( key, value );
    }

    private Object getViewCache( final GraphView view, final String key )
    {
        final Map<String, Object> viewCaches = caches.get( view );
        return viewCaches == null ? null : viewCaches.get( key );
    }

    @Override
    public boolean hasVariableProjects( final GraphView view )
    {
        return !getVariableProjects( view ).isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final GraphView view )
    {
        return findVisibleProjects( getRoots( view, false ), view.getFilter(), view, variableSubgraphs );
    }

    @Override
    public boolean addCycle( final EProjectCycle cycle )
    {
        boolean changed = false;
        synchronized ( this.cycles )
        {
            changed = this.cycles.add( cycle );
        }

        for ( final ProjectRelationship<?> rel : cycle )
        {
            incompleteSubgraphs.remove( rel.getDeclaring() );
        }

        return changed;
    }

    // TODO: May not work with paths to the entries in the cycle...since filters are often path-sensitive
    @Override
    public Set<EProjectCycle> getCycles( final GraphView view )
    {
        final Set<EProjectCycle> result = new HashSet<EProjectCycle>();
        if ( view.getFilter() == null )
        {
            result.addAll( cycles );
        }
        else
        {
            final ProjectRelationshipFilter filter = view.getFilter();
            nextCycle: for ( final EProjectCycle cycle : cycles )
            {
                for ( final ProjectRelationship<?> r : cycle )
                {
                    if ( !filter.accept( r ) )
                    {
                        continue nextCycle;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : cycles )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : cycles )
        {
            if ( cycle.contains( ref.asProjectVersionRef() ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        return getMetadata( ref, null );
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref, final Set<String> keys )
    {
        Map<String, String> metadata;
        synchronized ( this )
        {
            metadata = this.metadata.get( ref.asProjectVersionRef() );
            if ( metadata == null )
            {
                metadata = new HashMap<String, String>();
                this.metadata.put( ref.asProjectVersionRef(), metadata );
            }
        }

        if ( keys != null && !keys.isEmpty() )
        {
            metadata = new HashMap<>( metadata );
            final Set<String> removable = new HashSet<>( metadata.keySet() );
            removable.removeAll( keys );

            for ( final String remove : removable )
            {
                metadata.remove( remove );
            }
        }

        return metadata;
    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final String key, final String value )
    {
        if ( StringUtils.isEmpty( key ) || StringUtils.isEmpty( value ) )
        {
            return;
        }

        final Map<String, String> md = getMetadata( ref.asProjectVersionRef() );
        md.put( key, value );

        addMetadataOwner( key, ref.asProjectVersionRef() );
    }

    private synchronized void addMetadataOwner( final String key, final ProjectVersionRef ref )
    {
        Set<ProjectVersionRef> owners = this.metadataOwners.get( key );
        if ( owners == null )
        {
            owners = new HashSet<ProjectVersionRef>();
            metadataOwners.put( key, owners );
        }

        owners.add( ref.asProjectVersionRef() );
    }

    @Override
    public void setMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        if ( metadata == null || metadata.isEmpty() )
        {
            return;
        }

        final Map<String, String> md = getMetadata( ref.asProjectVersionRef() );
        md.putAll( metadata );
    }

    @Override
    public synchronized void reindex()
        throws GraphDriverException
    {
        for ( final Map.Entry<ProjectVersionRef, Map<String, String>> refEntry : metadata.entrySet() )
        {
            for ( final Map.Entry<String, String> mdEntry : refEntry.getValue()
                                                                    .entrySet() )
            {
                addMetadataOwner( mdEntry.getKey(), refEntry.getKey()
                                                            .asProjectVersionRef() );
            }
        }
    }

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
    {
        return metadataOwners.get( key );
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !graph.containsVertex( ref.asProjectVersionRef() ) )
        {
            graph.addVertex( ref.asProjectVersionRef() );
        }
    }

    @Override
    public void selectVersionFor( final ProjectVersionRef ref, final ProjectVersionRef selected )
    {
        this.selected.put( ref.asProjectVersionRef(), selected );
    }

    @Override
    public void selectVersionForAll( final ProjectRef ref, final ProjectVersionRef selected )
    {
        selectedForAll.put( ref.asProjectRef(), selected );
    }

    @Override
    public boolean clearSelectedVersions()
    {
        selected.clear();
        selectedForAll.clear();
        return true;
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo, final RelationshipType... types )
    {
        return getMatchingRelationships( graph.getOutEdges( from.asProjectVersionRef() ), view, includeManagedInfo, types );
    }

    private Set<ProjectRelationship<?>> getMatchingRelationships( final Collection<ProjectRelationship<?>> edges, final GraphView view,
                                                                  final boolean includeManagedInfo, final RelationshipType... types )
    {
        if ( edges == null )
        {
            // logger.info( "No edges found. Nothing to filter!" );
            return null;
        }

        // logger.info( "Filtering %d edges...", edges.size() );
        final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>( edges.size() );

        final List<RelationshipType> typeList = Arrays.asList( types );
        Collections.sort( typeList );

        for ( final ProjectRelationship<?> rel : edges )
        {
            if ( !typeList.isEmpty() && !typeList.contains( rel.getType() ) )
            {
                // logger.info( "-= %s (wrong type)", rel );
                continue;
            }

            if ( view.getFilter() != null && !view.getFilter()
                                                  .accept( rel ) )
            {
                // logger.info( "-= %s (rejected by filter)", rel );
                continue;
            }

            if ( !includeManagedInfo && rel.isManaged() )
            {
                // logger.info( "-= %s (wrong managed status)", rel );
                continue;
            }

            // logger.info( "+= %s", rel );
            rels.add( rel );
        }

        return rels;
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        // logger.info( "Getting relationships targeting: %s (types: %s)", to, join( types, ", " ) );
        return getMatchingRelationships( graph.getInEdges( to.asProjectVersionRef() ), view, includeManagedInfo, types );
    }

    @Override
    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphView eProjectNetView )
    {
        return byGA.containsKey( projectRef.asProjectRef() ) ? byGA.get( projectRef.asProjectRef() ) : Collections.<ProjectVersionRef> emptySet();
    }

    @Override
    public ProjectVersionRef getSelectedFor( final ProjectVersionRef ref )
    {
        return getSelectedVersion( ref.asProjectVersionRef() );
    }

    @Override
    public Map<ProjectVersionRef, ProjectVersionRef> getSelections()
    {
        return selected;
    }

    @Override
    public boolean hasSelectionFor( final ProjectVersionRef ref )
    {
        return selected.containsKey( ref.asProjectVersionRef() );
    }

    @Override
    public boolean hasSelectionForAll( final ProjectRef ref )
    {
        return selectedForAll.containsKey( ref.asProjectRef() );
    }

    @Override
    public Map<ProjectRef, ProjectVersionRef> getWildcardSelections()
    {
        return selectedForAll;
    }

}
