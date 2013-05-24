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
package org.commonjava.maven.atlas.spi.jung.effective;

import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
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
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.AbstractProjectRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipComparator;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.apache.maven.graph.effective.session.EGraphSession;
import org.apache.maven.graph.effective.traverse.AbstractTraversal;
import org.apache.maven.graph.effective.traverse.FilteringTraversal;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.effective.util.RelationshipUtils;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.util.logging.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class JungEGraphDriver
    implements EGraphDriver
{
    //    private final Logger logger = new Logger( getClass() );

    private DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();

    private transient Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> variableSubgraphs = new HashSet<ProjectVersionRef>();

    //    private transient Map<ProjectVersionRef, ProjectVersionRef> selected =
    //        new HashMap<ProjectVersionRef, ProjectVersionRef>();
    //
    //    private transient Map<ProjectRelationship<?>, ProjectRelationship<?>> replaced =
    //        new HashMap<ProjectRelationship<?>, ProjectRelationship<?>>();
    //
    private final Map<String, Set<ProjectVersionRef>> metadataOwners = new HashMap<String, Set<ProjectVersionRef>>();

    private final Map<ProjectVersionRef, Map<String, String>> metadata =
        new HashMap<ProjectVersionRef, Map<String, String>>();

    private transient Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private ProjectVersionRef[] roots;

    private final EGraphSession session;

    public JungEGraphDriver()
    {
        this.session = null;
    }

    public JungEGraphDriver( final JungEGraphDriver from, final ProjectRelationshipFilter filter,
                             final EProjectNet net, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        this.roots = roots;
        this.session = net.getSession();
        Collection<ProjectRelationship<?>> rels;
        if ( filter != null && roots.length > 0 )
        {
            rels = filterRelationships( filter, net, roots );
        }
        else
        {
            rels = from.getAllRelationships();
        }

        addRelationships( rels.toArray( new ProjectRelationship<?>[] {} ) );

        for ( final ProjectVersionRef ref : from.incompleteSubgraphs )
        {
            if ( graph.containsVertex( ref ) )
            {
                incompleteSubgraphs.add( ref );
            }
        }

        for ( final ProjectVersionRef ref : from.variableSubgraphs )
        {
            if ( graph.containsVertex( ref ) )
            {
                variableSubgraphs.add( ref );
            }
        }

        for ( final Map.Entry<ProjectVersionRef, Map<String, String>> entry : from.metadata.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();

            if ( graph.containsVertex( ref ) )
            {
                metadata.put( ref, new HashMap<String, String>( entry.getValue() ) );
            }
        }
    }

    private Set<ProjectRelationship<?>> filterRelationships( final ProjectRelationshipFilter filter,
                                                             final EProjectNet net, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        final FilteringTraversal traversal = new FilteringTraversal( filter, true );
        for ( final ProjectVersionRef root : roots )
        {
            traverse( traversal, net, root );
        }

        return new HashSet<ProjectRelationship<?>>( traversal.getCapturedRelationships() );
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final ProjectVersionRef ref )
    {
        return imposeSelections( graph.getOutEdges( ref ) );
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        return imposeSelections( graph.getInEdges( ref ) );
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        return imposeSelections( graph.getEdges() );
    }

    private Collection<ProjectRelationship<?>> imposeSelections( final Collection<ProjectRelationship<?>> edges )
    {
        if ( edges == null || edges.isEmpty() )
        {
            return edges;
        }

        final List<ProjectRelationship<?>> result = new ArrayList<ProjectRelationship<?>>( edges.size() );
        for ( final ProjectRelationship<?> edge : edges )
        {
            final ProjectVersionRef target = edge.getTarget();
            final SingleVersion selected = session.getSelectedVersion( target );
            if ( selected != null )
            {
                result.add( edge.selectTarget( selected ) );
            }
            else
            {
                result.add( edge );
            }
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
            }

            if ( !graph.containsEdge( rel ) )
            {
                graph.addEdge( rel, rel.getDeclaring(), target );
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

            dfsTraverse( traversal, 0, rel.getTarget()
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

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final ProjectVersionRef... refs )
    {
        final PathDetectionTraversal traversal = new PathDetectionTraversal( refs );

        if ( roots == null )
        {
            new Logger( getClass() ).warn( "Cannot retrieve paths targeting %s. No roots specified for this project network!",
                                           join( refs, ", " ) );
            return null;
        }

        for ( final ProjectVersionRef root : roots )
        {
            dfsTraverse( traversal, 0, root );
        }

        return traversal.getPaths();
    }

    @Override
    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        final CycleDetectionTraversal traversal = new CycleDetectionTraversal( rel );

        dfsTraverse( traversal, 0, rel.getTarget()
                                      .asProjectVersionRef() );

        return !traversal.getCycles()
                         .isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects()
    {
        return new HashSet<ProjectVersionRef>( graph.getVertices() );
    }

    @Override
    public void traverse( final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
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
                    bfsTraverse( traversal, i, root );
                    break;
                }
                case depth_first:
                {
                    dfsTraverse( traversal, i, root );
                    break;
                }
            }

            traversal.endTraverse( i, net );
        }
    }

    // TODO: Implement without recursion.
    private void dfsTraverse( final ProjectNetTraversal traversal, final int pass, final ProjectVersionRef root )
    {
        dfsIterate( root, traversal, new LinkedList<ProjectRelationship<?>>(), pass );
    }

    private void dfsIterate( final ProjectVersionRef node, final ProjectNetTraversal traversal,
                             final LinkedList<ProjectRelationship<?>> path, final int pass )
    {
        final List<ProjectRelationship<?>> edges = getSortedOutEdges( node );
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
                            dfsIterate( target, traversal, path, pass );
                            path.removeLast();
                        }
                    }

                    traversal.edgeTraversed( edge, path, pass );
                }
            }
        }
    }

    // TODO: Implement without recursion.
    private void bfsTraverse( final ProjectNetTraversal traversal, final int pass, final ProjectVersionRef root )
    {
        final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
        path.add( new SelfEdge( root ) );

        bfsIterate( Collections.singletonList( path ), traversal, pass );
    }

    private void bfsIterate( final List<List<ProjectRelationship<?>>> thisLayer, final ProjectNetTraversal traversal,
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

            final List<ProjectRelationship<?>> edges = getSortedOutEdges( node );
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
            bfsIterate( nextLayer, traversal, pass );
        }
    }

    private List<ProjectRelationship<?>> getSortedOutEdges( final ProjectVersionRef node )
    {
        Collection<ProjectRelationship<?>> unsorted = graph.getOutEdges( node );
        if ( unsorted == null )
        {
            return null;
        }

        unsorted = new ArrayList<ProjectRelationship<?>>( unsorted );

        RelationshipUtils.filterTerminalParents( unsorted );

        final List<ProjectRelationship<?>> sorted =
            new ArrayList<ProjectRelationship<?>>( imposeSelections( unsorted ) );
        Collections.sort( sorted, new RelationshipComparator() );

        return sorted;
    }

    private static final class SelfEdge
        extends AbstractProjectRelationship<ProjectVersionRef>
    {

        private static final long serialVersionUID = 1L;

        SelfEdge( final ProjectVersionRef ref )
        {
            super( null, null, ref, ref, 0 );
        }

        @Override
        public ArtifactRef getTargetArtifact()
        {
            return new ArtifactRef( getTarget(), "pom", null, false );
        }

        @Override
        public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
        {
            return new SelfEdge( getDeclaring().selectVersion( version ) );
        }

        @Override
        public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
        {
            return new SelfEdge( getDeclaring().selectVersion( version ) );
        }

    }

    @Override
    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectRelationshipFilter filter,
                                         final ProjectVersionRef... from )
        throws GraphDriverException
    {
        final JungEGraphDriver neo = new JungEGraphDriver( this, filter, net, from );
        neo.restrictProjectMembership( Arrays.asList( from ) );

        return neo;
    }

    @Override
    public EGraphDriver newInstance()
    {
        return new JungEGraphDriver();
    }

    @Override
    public boolean containsProject( final ProjectVersionRef ref )
    {
        return graph.containsVertex( ref );
    }

    @Override
    public boolean containsRelationship( final ProjectRelationship<?> rel )
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

    @Override
    public boolean isDerivedFrom( final EGraphDriver driver )
    {
        return false;
    }

    @Override
    public boolean isMissing( final ProjectVersionRef project )
    {
        return !graph.containsVertex( project );
    }

    @Override
    public boolean hasMissingProjects()
    {
        return !incompleteSubgraphs.isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects()
    {
        return new HashSet<ProjectVersionRef>( incompleteSubgraphs );
    }

    @Override
    public boolean hasVariableProjects()
    {
        return !variableSubgraphs.isEmpty();
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects()
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
    public Set<EProjectCycle> getCycles()
    {
        return new HashSet<EProjectCycle>( cycles );
    }

    @Override
    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
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
    public boolean isCycleParticipant( final ProjectVersionRef ref )
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
        for ( final ProjectVersionRef vertex : getAllProjects() )
        {
            final Collection<? extends ProjectRelationship<?>> outEdges = getRelationshipsDeclaredBy( vertex );
            if ( outEdges != null && !outEdges.isEmpty() )
            {
                incompleteSubgraphs.remove( vertex );
            }
        }
    }

    @Override
    public Map<String, String> getProjectMetadata( final ProjectVersionRef ref )
    {
        return metadata.get( ref );
    }

    @Override
    public void addProjectMetadata( final ProjectVersionRef ref, final String key, final String value )
    {
        if ( StringUtils.isEmpty( key ) || StringUtils.isEmpty( value ) )
        {
            return;
        }

        final Map<String, String> md = getMetadata( ref );
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
    public void addProjectMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        if ( metadata == null || metadata.isEmpty() )
        {
            return;
        }

        final Map<String, String> md = getMetadata( ref );
        md.putAll( metadata );
    }

    private synchronized Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        Map<String, String> metadata = this.metadata.get( ref );
        if ( metadata == null )
        {
            metadata = new HashMap<String, String>();
            this.metadata.put( ref, metadata );
        }

        return metadata;
    }

    public boolean includeGraph( final ProjectVersionRef project )
    {
        throw new UnsupportedOperationException(
                                                 "need to implement notion of a global graph in jung before this can work." );
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
    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
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
        public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
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
        public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
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
    public Set<ProjectVersionRef> getRoots()
    {
        return new HashSet<ProjectVersionRef>( Arrays.asList( roots ) );
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !graph.containsVertex( ref ) )
        {
            graph.addVertex( ref );
        }
    }

}
