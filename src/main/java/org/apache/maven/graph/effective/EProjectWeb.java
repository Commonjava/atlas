/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.AbstractProjectRelationship;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipComparator;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Graphs;

public class EProjectWeb
    implements EProjectNet, Serializable
{

    private static final long serialVersionUID = 1L;

    private transient Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> connectedProjects = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> variableSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();

    public EProjectWeb()
    {
    }

    public EProjectWeb( final Collection<ProjectRelationship<?>> relationships,
                        final Collection<EProjectRelationships> projectRelationships )
    {
        addAll( relationships );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }
    }

    public EProjectWeb( final Set<ProjectRelationship<?>> relationships )
    {
        addAll( relationships );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getAllRelationships()
     */
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( graph.getEdges() );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getRawGraph()
     */
    public DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> getRawGraph()
    {
        return Graphs.unmodifiableDirectedGraph( graph );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isComplete()
     */
    public boolean isComplete()
    {
        return incompleteSubgraphs.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#isConcrete()
     */
    public boolean isConcrete()
    {
        return variableSubgraphs.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getIncompleteSubgraphs()
     */
    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( incompleteSubgraphs );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#getVariableSubgraphs()
     */
    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( variableSubgraphs );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#add(org.apache.maven.graph.effective.EProjectRelationships)
     */
    public void add( final EProjectRelationships rels )
    {
        if ( incompleteSubgraphs.contains( rels.getProjectRef() ) )
        {
            incompleteSubgraphs.remove( rels.getProjectRef() );
        }

        connectedProjects.add( rels.getProjectRef() );

        addAll( rels.getAllRelationships() );
    }

    public <T extends ProjectRelationship<?>> void add( final T rel )
    {
        if ( rel == null )
        {
            return;
        }

        incompleteSubgraphs.remove( rel.getDeclaring() );

        ProjectVersionRef target = rel.getTarget();
        if ( rel instanceof DependencyRelationship )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        if ( !graph.containsVertex( target ) )
        {
            graph.addVertex( target );
        }

        graph.addEdge( rel, rel.getDeclaring(), target );

        if ( !target.getVersionSpec()
                    .isSingle() )
        {
            variableSubgraphs.add( target );
        }
        else if ( !connectedProjects.contains( target ) )
        {
            incompleteSubgraphs.add( target );
        }
    }

    public <T extends ProjectRelationship<?>> void addAll( final Collection<T> rels )
    {
        if ( rels == null )
        {
            return;
        }

        for ( final T rel : rels )
        {
            add( rel );
        }

        recomputeIncompleteSubgraphs();
    }

    public <T extends ProjectRelationship<?>> void addAll( final T... rels )
    {
        if ( rels == null )
        {
            return;
        }

        for ( final T rel : rels )
        {
            add( rel );
        }

        recomputeIncompleteSubgraphs();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#connect(org.apache.maven.graph.effective.EProjectWeb)
     */
    public void connect( final EProjectWeb otherWeb )
    {
        final DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> otherGraph = otherWeb.getRawGraph();
        final Collection<ProjectVersionRef> otherNodes = otherGraph.getVertices();
        for ( final ProjectVersionRef node : otherNodes )
        {
            final Collection<ProjectRelationship<?>> outEdges = otherGraph.getOutEdges( node );
            if ( incompleteSubgraphs.contains( node ) && outEdges != null && !outEdges.isEmpty() )
            {
                incompleteSubgraphs.remove( node );
            }
        }

        final Set<ProjectVersionRef> otherIncomplete = otherWeb.getIncompleteSubgraphs();
        for ( final ProjectVersionRef node : otherIncomplete )
        {
            if ( incompleteSubgraphs.contains( node ) )
            {
                continue;
            }

            if ( graph.containsVertex( node ) )
            {
                final Collection<ProjectRelationship<?>> outEdges = graph.getOutEdges( node );
                if ( outEdges == null || outEdges.isEmpty() )
                {
                    incompleteSubgraphs.add( node );
                }
            }
            else
            {
                incompleteSubgraphs.add( node );
            }
        }

        this.connectedProjects.addAll( otherWeb.connectedProjects );
        addAll( otherWeb.getAllRelationships() );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.graph.effective.EProjectNetwork#traverse(org.apache.maven.graph.common.ref.ProjectVersionRef, org.apache.maven.graph.effective.traverse.ProjectGraphTraversal)
     */
    public void traverse( final ProjectVersionRef start, final ProjectNetTraversal traversal )
    {
        final int passes = traversal.getRequiredPasses();
        for ( int i = 0; i < passes; i++ )
        {
            traversal.startTraverse( i, this );

            switch ( traversal.getType( i ) )
            {
                case breadth_first:
                {
                    bfsTraverse( start, traversal, i );
                    break;
                }
                case depth_first:
                {
                    dfsTraverse( start, traversal, i );
                    break;
                }
            }

            traversal.endTraverse( i, this );
        }
    }

    // TODO: Implement without recursion.
    private void dfsTraverse( final ProjectVersionRef start, final ProjectNetTraversal traversal, final int pass )
    {
        dfsIterate( start, traversal, new LinkedList<ProjectRelationship<?>>(), pass );
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
                    path.addLast( edge );

                    ProjectVersionRef target = edge.getTarget();
                    if ( target instanceof ArtifactRef )
                    {
                        target = ( (ArtifactRef) target ).asProjectVersionRef();
                    }

                    dfsIterate( target, traversal, path, pass );
                    path.removeLast();

                    traversal.edgeTraversed( edge, path, pass );
                }
            }
        }
    }

    // TODO: Implement without recursion.
    private void bfsTraverse( final ProjectVersionRef start, final ProjectNetTraversal traversal, final int pass )
    {
        final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
        path.add( new SelfEdge( start ) );

        bfsIterate( Collections.singletonList( path ), traversal, pass );
    }

    private void bfsIterate( final List<List<ProjectRelationship<?>>> thisLayer, final ProjectNetTraversal traversal,
                             final int pass )
    {
        final List<List<ProjectRelationship<?>>> nextLayer = new ArrayList<List<ProjectRelationship<?>>>();

        for ( final List<ProjectRelationship<?>> path : thisLayer )
        {
            ProjectVersionRef node = path.get( path.size() - 1 )
                                         .getTarget();
            if ( node instanceof ArtifactRef )
            {
                node = ( (ArtifactRef) node ).asProjectVersionRef();
            }

            if ( !path.isEmpty() && ( path.get( 0 ) instanceof SelfEdge ) )
            {
                path.remove( 0 );
            }

            final List<ProjectRelationship<?>> edges = getSortedOutEdges( node );
            if ( edges != null )
            {
                for ( final ProjectRelationship<?> edge : edges )
                {
                    if ( ( edge instanceof SelfEdge ) || traversal.traverseEdge( edge, path, pass ) )
                    {
                        final List<ProjectRelationship<?>> nextPath = new ArrayList<ProjectRelationship<?>>( path );

                        nextPath.add( edge );
                        nextLayer.add( nextPath );

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
        final Collection<ProjectRelationship<?>> unsorted = graph.getOutEdges( node );
        if ( unsorted != null )
        {
            final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( unsorted );
            Collections.sort( sorted, new RelationshipComparator() );

            return sorted;
        }

        return null;
    }

    private static final class SelfEdge
        extends AbstractProjectRelationship<ProjectVersionRef>
    {

        private static final long serialVersionUID = 1L;

        SelfEdge( final ProjectVersionRef ref )
        {
            super( null, ref, ref, 0 );
        }

        @Override
        public ArtifactRef getTargetArtifact()
        {
            return new ArtifactRef( getTarget(), "pom", null, false );
        }

    }

    public Set<ProjectRelationship<?>> getUserRelationships( final ProjectVersionRef ref )
    {
        if ( !graph.containsVertex( ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( graph.getInEdges( ref ) );
    }

    public Set<ProjectRelationship<?>> getDirectRelationships( final ProjectVersionRef ref )
    {
        if ( !graph.containsVertex( ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( graph.getOutEdges( ref ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        for ( final ProjectVersionRef ref : graph.getVertices() )
        {
            final Collection<ProjectRelationship<?>> inEdges = graph.getInEdges( ref );
            if ( inEdges == null || inEdges.isEmpty() )
            {
                result.add( ref );
            }
        }

        return result;
    }

    private void readObject( final java.io.ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        incompleteSubgraphs = new HashSet<ProjectVersionRef>();
        connectedProjects = new HashSet<ProjectVersionRef>();
        variableSubgraphs = new HashSet<ProjectVersionRef>();
    }

    public void recomputeIncompleteSubgraphs()
    {
        for ( final ProjectVersionRef vertex : graph.getVertices() )
        {
            incompleteSubgraphs.remove( vertex );
        }
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        return getAllRelationships();
    }

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

    public void addCycle( final EProjectCycle cycle )
    {
        this.cycles.add( cycle );
    }

    public Set<EProjectCycle> getCycles()
    {
        return new HashSet<EProjectCycle>( cycles );
    }

    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<ProjectRelationship<?>> rels = graph.getInEdges( ref );
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }
}
