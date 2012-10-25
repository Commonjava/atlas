package org.apache.maven.graph.effective;

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
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipComparator;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Graphs;

public class EProjectWeb
    implements EProjectNet, EProjectRelationshipCollection
{

    private static final long serialVersionUID = 1L;

    private transient final Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<ProjectVersionRef>();

    private transient final Set<ProjectVersionRef> connectedProjects = new HashSet<ProjectVersionRef>();

    private transient final Set<ProjectVersionRef> variableSubgraphs = new HashSet<ProjectVersionRef>();

    private final DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();

    public EProjectWeb()
    {
    }

    public EProjectWeb( final Collection<ParentRelationship> parents,
                        final Collection<DependencyRelationship> dependencies,
                        final Collection<PluginRelationship> plugins,
                        final Collection<PluginDependencyRelationship> pluginLevelDeps,
                        final Collection<ExtensionRelationship> extensions,
                        final Collection<EProjectRelationships> projectRelationships )
    {
        addAll( parents );
        addAll( dependencies );
        addAll( plugins );
        addAll( pluginLevelDeps );
        addAll( extensions );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }
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

}
