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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.AbstractProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipComparator;
import org.commonjava.maven.atlas.graph.rel.RelationshipPathComparator;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.util.logging.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class JungEGraphDriver
    implements GraphDatabaseDriver
{
    //    private final Logger logger = new Logger( getClass() );

    private DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph = new DirectedSparseMultigraph<>();

    private final Map<ProjectRef, Set<ProjectVersionRef>> byGA = new HashMap<>();

    private transient Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<>();

    private transient Set<ProjectVersionRef> variableSubgraphs = new HashSet<>();

    private transient Map<ProjectVersionRef, ProjectVersionRef> selected = new HashMap<>();

    private transient Map<ProjectRef, ProjectVersionRef> selectedForAll = new HashMap<>();

    private final Map<String, Set<ProjectVersionRef>> metadataOwners = new HashMap<>();

    private final Map<ProjectVersionRef, Map<String, String>> metadata = new HashMap<>();

    private final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final GraphView view, final ProjectVersionRef ref )
    {
        return imposeSelections( view, graph.getOutEdges( ref ) );
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final GraphView view, final ProjectVersionRef ref )
    {
        return imposeSelections( view, graph.getInEdges( ref ) );
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        return imposeSelections( view, graph.getEdges() );
    }

    private Collection<ProjectRelationship<?>> imposeSelections( final GraphView view, final Collection<ProjectRelationship<?>> edges )
    {
        if ( edges == null || edges.isEmpty() )
        {
            return edges;
        }

        final GraphWorkspace workspace = view.getWorkspace();
        if ( workspace == null )
        {
            // no selections here...
            return edges;
        }

        final List<ProjectRelationship<?>> result = new ArrayList<ProjectRelationship<?>>( edges.size() );
        for ( final ProjectRelationship<?> edge : edges )
        {
            final ProjectVersionRef target = edge.getTarget();
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
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( !graph.containsVertex( rel.getDeclaring() ) )
            {
                graph.addVertex( rel.getDeclaring() );
                addGA( rel.getDeclaring() );
            }

            final ProjectVersionRef target = rel.getTarget()
                                                .asProjectVersionRef();
            if ( !target.getVersionSpec()
                        .isSingle() )
            {
                variableSubgraphs.add( target );
            }
            else if ( !graph.containsVertex( target ) )
            {
                incompleteSubgraphs.add( target );
            }

            if ( !graph.containsVertex( target ) )
            {
                graph.addVertex( target );
                addGA( target );
            }

            final List<ProjectRelationship<?>> edges = new ArrayList<ProjectRelationship<?>>( graph.findEdgeSet( rel.getDeclaring(), target ) );
            if ( !edges.contains( rel ) )
            {
                graph.addEdge( rel, rel.getDeclaring(), target );
            }
            else
            {
                final int idx = edges.indexOf( rel );
                final ProjectRelationship<?> existing = edges.get( idx );
                existing.addSources( rel.getSources() );
            }

            incompleteSubgraphs.remove( rel.getDeclaring() );
        }

        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( skipped.contains( rel ) )
            {
                continue;
            }

            final CycleDetectionTraversal traversal = new CycleDetectionTraversal( rel );

            dfsTraverse( GraphView.GLOBAL, traversal, 0, rel.getTarget()
                                                            .asProjectVersionRef() );

            final List<EProjectCycle> cycles = traversal.getCycles();

            if ( !cycles.isEmpty() )
            {
                skipped.add( rel );

                graph.removeEdge( rel );
                this.cycles.addAll( cycles );
            }
        }

        return skipped;
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
        final PathDetectionTraversal traversal = new PathDetectionTraversal( refs );

        final Set<ProjectVersionRef> roots = view.getRoots();
        if ( roots == null )
        {
            new Logger( getClass() ).warn( "Cannot retrieve paths targeting %s. No roots specified for this project network!", join( refs, ", " ) );
            return null;
        }

        for ( final ProjectVersionRef root : roots )
        {
            dfsTraverse( view, traversal, 0, root );
        }

        return traversal.getPaths();
    }

    @Override
    public boolean introducesCycle( final GraphView view, final ProjectRelationship<?> rel )
    {
        final CycleDetectionTraversal traversal = new CycleDetectionTraversal( rel );

        dfsTraverse( view, traversal, 0, rel.getTarget()
                                            .asProjectVersionRef() );

        return !traversal.getCycles()
                         .isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        return new HashSet<ProjectVersionRef>( graph.getVertices() );
    }

    @Override
    public void traverse( final GraphView view, final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
        throws GraphDriverException
    {
        final int passes = traversal.getRequiredPasses();
        for ( int i = 0; i < passes; i++ )
        {
            traversal.startTraverse( i, net );

            switch ( traversal.getType( i ) )
            {
                case breadth_first:
                {
                    bfsTraverse( view, traversal, i, root );
                    break;
                }
                case depth_first:
                {
                    dfsTraverse( view, traversal, i, root );
                    break;
                }
            }

            traversal.endTraverse( i, net );
        }
    }

    // TODO: Implement without recursion.
    private void dfsTraverse( final GraphView view, final ProjectNetTraversal traversal, final int pass, final ProjectVersionRef root )
    {
        dfsIterate( view, root, traversal, new LinkedList<ProjectRelationship<?>>(), pass );
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
                            path.addLast( edge );
                            dfsIterate( view, target, traversal, path, pass );
                            path.removeLast();
                        }
                    }

                    traversal.edgeTraversed( edge, path, pass );
                }
            }
        }
    }

    // TODO: Implement without recursion.
    private void bfsTraverse( final GraphView view, final ProjectNetTraversal traversal, final int pass, final ProjectVersionRef root )
    {
        final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
        path.add( new SelfEdge( root ) );

        bfsIterate( view, Collections.singletonList( path ), traversal, pass );
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
        Collection<ProjectRelationship<?>> unsorted = graph.getOutEdges( node );
        if ( unsorted == null )
        {
            return null;
        }

        unsorted = new ArrayList<ProjectRelationship<?>>( unsorted );

        RelationshipUtils.filterTerminalParents( unsorted );

        final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( imposeSelections( view, unsorted ) );
        Collections.sort( sorted, new RelationshipComparator() );

        return sorted;
    }

    private static final class SelfEdge
        extends AbstractProjectRelationship<ProjectVersionRef>
    {

        private static final long serialVersionUID = 1L;

        SelfEdge( final ProjectVersionRef ref )
        {
            super( (URI) null, null, ref, ref, 0 );
        }

        @Override
        public ArtifactRef getTargetArtifact()
        {
            return new ArtifactRef( getTarget(), "pom", null, false );
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

    //    @Override
    //    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectRelationshipFilter filter,
    //                                         final ProjectVersionRef... from )
    //        throws GraphDriverException
    //    {
    //        final JungEGraphDriver neo = new JungEGraphDriver( this, filter, net, null, from );
    //        neo.restrictProjectMembership( Arrays.asList( from ) );
    //
    //        return neo;
    //    }
    //
    //    @Override
    //    public EGraphDriver newInstance( final EGraphSession workspace, final EProjectNet net,
    //                                     final ProjectRelationshipFilter filter, final ProjectVersionRef... from )
    //        throws GraphDriverException
    //    {
    //        final JungEGraphDriver neo = new JungEGraphDriver( this, filter, net, null, from );
    //        neo.restrictProjectMembership( Arrays.asList( from ) );
    //
    //        return neo;
    //    }

    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        return graph.containsVertex( ref );
    }

    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        return graph.containsEdge( rel );
    }

    public void restrictProjectMembership( final Collection<ProjectVersionRef> refs )
    {
        final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Collection<ProjectRelationship<?>> edges = graph.getOutEdges( ref );
            if ( edges != null )
            {
                rels.addAll( edges );
            }
        }

        restrictRelationshipMembership( rels );
    }

    public void restrictRelationshipMembership( final Collection<ProjectRelationship<?>> rels )
    {
        graph = new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();
        incompleteSubgraphs.clear();
        variableSubgraphs.clear();

        addRelationships( rels.toArray( new ProjectRelationship<?>[] {} ) );

        recomputeIncompleteSubgraphs();
    }

    @Override
    public void close()
        throws IOException
    {
        // NOP; stored in memory.
    }

    //    @Override
    //    public boolean isDerivedFrom( final EGraphDriver driver )
    //    {
    //        return false;
    //    }

    @Override
    public boolean isMissing( final GraphView view, final ProjectVersionRef project )
    {
        return !graph.containsVertex( project );
    }

    @Override
    public boolean hasMissingProjects( final GraphView view )
    {
        return !incompleteSubgraphs.isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects( final GraphView view )
    {
        return new HashSet<ProjectVersionRef>( incompleteSubgraphs );
    }

    @Override
    public boolean hasVariableProjects( final GraphView view )
    {
        return !variableSubgraphs.isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final GraphView view )
    {
        return new HashSet<ProjectVersionRef>( variableSubgraphs );
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
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void recomputeIncompleteSubgraphs()
    {
        for ( final ProjectVersionRef vertex : getAllProjects( GraphView.GLOBAL ) )
        {
            final Collection<? extends ProjectRelationship<?>> outEdges = getRelationshipsDeclaredBy( GraphView.GLOBAL, vertex );
            if ( outEdges != null && !outEdges.isEmpty() )
            {
                incompleteSubgraphs.remove( vertex );
            }
        }
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        return metadata.get( ref );
    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final String key, final String value )
    {
        if ( StringUtils.isEmpty( key ) || StringUtils.isEmpty( value ) )
        {
            return;
        }

        final Map<String, String> md = getMetadataMap( ref );
        md.put( key, value );

        addMetadataOwner( key, ref );
    }

    private synchronized void addMetadataOwner( final String key, final ProjectVersionRef ref )
    {
        Set<ProjectVersionRef> owners = this.metadataOwners.get( key );
        if ( owners == null )
        {
            owners = new HashSet<ProjectVersionRef>();
            metadataOwners.put( key, owners );
        }

        owners.add( ref );
    }

    @Override
    public void setMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        if ( metadata == null || metadata.isEmpty() )
        {
            return;
        }

        final Map<String, String> md = getMetadataMap( ref );
        md.putAll( metadata );
    }

    private synchronized Map<String, String> getMetadataMap( final ProjectVersionRef ref )
    {
        Map<String, String> metadata = this.metadata.get( ref );
        if ( metadata == null )
        {
            metadata = new HashMap<String, String>();
            this.metadata.put( ref, metadata );
        }

        return metadata;
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
                addMetadataOwner( mdEntry.getKey(), refEntry.getKey() );
            }
        }
    }

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
    {
        return metadataOwners.get( key );
    }

    //    public void selectVersionFor( final ProjectVersionRef variable, final ProjectVersionRef select )
    //        throws GraphDriverException
    //    {
    //        if ( !select.isSpecificVersion() )
    //        {
    //            throw new GraphDriverException( "Cannot select non-concrete version! Attempted to select: %s", select );
    //        }
    //
    //        if ( variable.isSpecificVersion() )
    //        {
    //            throw new GraphDriverException(
    //                                            "Cannot select version if target is already a concrete version! Attempted to select for: %s",
    //                                            variable );
    //        }
    //
    //        selected.put( variable, select );
    //
    //        // Don't worry about selecting for outbound edges, as those subgraphs are supposed to be the same...
    //        final Collection<ProjectRelationship<?>> rels = graph.getInEdges( variable );
    //        for ( final ProjectRelationship<?> rel : rels )
    //        {
    //
    //            ProjectRelationship<?> repl;
    //            if ( rel.getTarget()
    //                    .asProjectVersionRef()
    //                    .equals( variable ) )
    //            {
    //                repl = rel.selectTarget( (SingleVersion) select.getVersionSpec() );
    //            }
    //            else
    //            {
    //                continue;
    //            }
    //
    //            graph.removeEdge( rel );
    //            graph.addEdge( repl, repl.getDeclaring(), repl.getTarget()
    //                                                          .asProjectVersionRef() );
    //
    //            replaced.put( rel, repl );
    //        }
    //    }
    //
    //    public Map<ProjectVersionRef, ProjectVersionRef> clearSelectedVersions()
    //    {
    //        final Map<ProjectVersionRef, ProjectVersionRef> selected =
    //            new HashMap<ProjectVersionRef, ProjectVersionRef>( this.selected );
    //
    //        selected.clear();
    //
    //        for ( final Map.Entry<ProjectRelationship<?>, ProjectRelationship<?>> entry : replaced.entrySet() )
    //        {
    //            final ProjectRelationship<?> rel = entry.getKey();
    //            final ProjectRelationship<?> repl = entry.getValue();
    //
    //            graph.removeEdge( repl );
    //            graph.addEdge( rel, rel.getDeclaring(), rel.getTarget()
    //                                                       .asProjectVersionRef() );
    //        }
    //
    //        for ( final ProjectVersionRef select : new HashSet<ProjectVersionRef>( selected.values() ) )
    //        {
    //            final Collection<ProjectRelationship<?>> edges = graph.getInEdges( select );
    //            if ( edges.isEmpty() )
    //            {
    //                graph.removeVertex( select );
    //            }
    //        }
    //
    //        return selected;
    //    }
    //
    //    public Map<ProjectVersionRef, ProjectVersionRef> getSelectedVersions()
    //    {
    //        return selected;
    //    }

    private static final class CycleDetectionTraversal
        extends AbstractTraversal
    {
        private final List<EProjectCycle> cycles = new ArrayList<EProjectCycle>();

        private final ProjectRelationship<?> rel;

        private CycleDetectionTraversal( final ProjectRelationship<?> rel )
        {
            this.rel = rel;
        }

        public List<EProjectCycle> getCycles()
        {
            return cycles;
        }

        @Override
        public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
        {
            if ( rel.getDeclaring()
                    .equals( rel.getTarget()
                                .asProjectVersionRef() ) )
            {
                return false;
            }

            new Logger( getClass() ).info( "Checking for cycle:\n\n%s\n\n", join( path, "\n" ) );

            final ProjectVersionRef from = rel.getDeclaring();
            if ( from.equals( relationship.getTarget()
                                          .asProjectVersionRef() ) )
            {
                final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>( path );
                cycle.add( rel );

                cycles.add( new EProjectCycle( cycle ) );
                return false;
            }

            return true;
        }
    }

    private static final class PathDetectionTraversal
        extends AbstractTraversal
    {
        private final ProjectVersionRef[] to;

        private final Set<List<ProjectRelationship<?>>> paths = new HashSet<List<ProjectRelationship<?>>>();

        private PathDetectionTraversal( final ProjectVersionRef[] refs )
        {
            this.to = refs;
        }

        public Set<List<ProjectRelationship<?>>> getPaths()
        {
            return paths;
        }

        @Override
        public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
        {
            final ProjectVersionRef target = relationship.getTarget()
                                                         .asProjectVersionRef();
            for ( final ProjectVersionRef t : to )
            {
                if ( t.equals( target ) )
                {
                    paths.add( new ArrayList<ProjectRelationship<?>>( path ) );
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !graph.containsVertex( ref ) )
        {
            graph.addVertex( ref );
        }
    }

    @Override
    public void selectVersionFor( final ProjectVersionRef ref, final ProjectVersionRef selected )
    {
        this.selected.put( ref, selected );
    }

    @Override
    public void selectVersionForAll( final ProjectRef ref, final ProjectVersionRef selected )
    {
        selectedForAll.put( ref, selected );
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
        return getMatchingRelationships( graph.getOutEdges( from ), view, includeManagedInfo, types );
    }

    private Set<ProjectRelationship<?>> getMatchingRelationships( final Collection<ProjectRelationship<?>> edges, final GraphView view,
                                                                  final boolean includeManagedInfo, final RelationshipType... types )
    {
        if ( edges == null )
        {
            return null;
        }

        final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>( edges.size() );

        final List<RelationshipType> typeList = Arrays.asList( types );
        Collections.sort( typeList );

        for ( final ProjectRelationship<?> rel : edges )
        {
            if ( !typeList.isEmpty() && !typeList.contains( rel.getType() ) )
            {
                continue;
            }

            if ( view.getFilter() != null && !view.getFilter()
                                                  .accept( rel ) )
            {
                continue;
            }

            if ( !includeManagedInfo && rel.isManaged() )
            {
                continue;
            }

            rels.add( rel );
        }

        return rels;
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        return getMatchingRelationships( graph.getInEdges( to ), view, includeManagedInfo, types );
    }

    @Override
    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphView eProjectNetView )
    {
        return byGA.containsKey( projectRef ) ? byGA.get( projectRef ) : Collections.<ProjectVersionRef> emptySet();
    }

    @Override
    public ProjectVersionRef getSelectedFor( final ProjectVersionRef ref )
    {
        return getSelectedVersion( ref );
    }

    @Override
    public Map<ProjectVersionRef, ProjectVersionRef> getSelections()
    {
        return selected;
    }

    @Override
    public boolean hasSelectionFor( final ProjectVersionRef ref )
    {
        return selected.containsKey( ref );
    }

    @Override
    public boolean hasSelectionForAll( final ProjectRef ref )
    {
        return selectedForAll.containsKey( ref );
    }

    @Override
    public Map<ProjectRef, ProjectVersionRef> getWildcardSelections()
    {
        return selectedForAll;
    }

}
