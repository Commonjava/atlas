package org.commonjava.maven.atlas.spi.jung.effective;

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
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.AbstractProjectRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipComparator;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.effective.util.EGraphUtils;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class JungEGraphDriver
    implements EGraphDriver
{

    private DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();

    private transient Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> variableSubgraphs = new HashSet<ProjectVersionRef>();

    private final Map<String, Set<ProjectVersionRef>> metadataOwners = new HashMap<String, Set<ProjectVersionRef>>();

    private final Map<ProjectVersionRef, Map<String, String>> metadata =
        new HashMap<ProjectVersionRef, Map<String, String>>();

    private transient Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    public JungEGraphDriver()
    {
    }

    public JungEGraphDriver( final JungEGraphDriver from )
    {
        incompleteSubgraphs.addAll( from.incompleteSubgraphs );
        variableSubgraphs.addAll( from.variableSubgraphs );

        for ( final Map.Entry<ProjectVersionRef, Map<String, String>> entry : from.metadata.entrySet() )
        {
            metadata.put( entry.getKey(), new HashMap<String, String>( entry.getValue() ) );
        }

        for ( final ProjectRelationship<?> rel : from.getAllRelationships() )
        {
            addRelationship( rel );
        }
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final ProjectVersionRef ref )
    {
        return graph.getOutEdges( ref );
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        return graph.getInEdges( ref );
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        return graph.getEdges();
    }

    public boolean addRelationship( final ProjectRelationship<?> rel )
    {
        boolean changed = false;
        if ( !graph.containsVertex( rel.getDeclaring() ) )
        {
            graph.addVertex( rel.getDeclaring() );
            changed = true;
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
            changed = true;
        }

        if ( !graph.containsEdge( rel ) )
        {
            graph.addEdge( rel, rel.getDeclaring(), target );
            changed = true;
        }

        incompleteSubgraphs.remove( rel.getDeclaring() );

        return changed;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return new HashSet<ProjectVersionRef>( graph.getVertices() );
    }

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
        throws GraphDriverException
    {
        dfsIterate( root, traversal, new LinkedList<ProjectRelationship<?>>(), pass );
    }

    private void dfsIterate( final ProjectVersionRef node, final ProjectNetTraversal traversal,
                             final LinkedList<ProjectRelationship<?>> path, final int pass )
        throws GraphDriverException
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
                        ProjectVersionRef target = edge.getTarget();
                        if ( target instanceof ArtifactRef )
                        {
                            target = ( (ArtifactRef) target ).asProjectVersionRef();
                        }

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
        throws GraphDriverException
    {
        final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
        path.add( new SelfEdge( root ) );

        bfsIterate( Collections.singletonList( path ), traversal, pass );
    }

    private void bfsIterate( final List<List<ProjectRelationship<?>>> thisLayer, final ProjectNetTraversal traversal,
                             final int pass )
        throws GraphDriverException
    {
        final List<List<ProjectRelationship<?>>> nextLayer = new ArrayList<List<ProjectRelationship<?>>>();

        for ( final List<ProjectRelationship<?>> path : thisLayer )
        {
            if ( path.isEmpty() )
            {
                continue;
            }

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

        EGraphUtils.filterTerminalParents( unsorted );

        final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( unsorted );
        Collections.sort( sorted, new RelationshipComparator() );

        return sorted;
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

    public EGraphDriver newInstanceFrom( final EProjectNet net, final ProjectVersionRef... from )
    {
        final JungEGraphDriver neo = new JungEGraphDriver( this );
        neo.restrictProjectMembership( Arrays.asList( from ) );

        return neo;
    }

    public EGraphDriver newInstance()
    {
        return new JungEGraphDriver();
    }

    public boolean containsProject( final ProjectVersionRef ref )
    {
        return graph.containsVertex( ref );
    }

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

        for ( final ProjectRelationship<?> rel : rels )
        {
            addRelationship( rel );
        }

        recomputeIncompleteSubgraphs();
    }

    public void close()
        throws IOException
    {
        // NOP; stored in memory.
    }

    public boolean isDerivedFrom( final EGraphDriver driver )
    {
        return false;
    }

    public boolean isMissing( final ProjectVersionRef project )
    {
        return !graph.containsVertex( project );
    }

    public boolean hasMissingProjects()
    {
        return !incompleteSubgraphs.isEmpty();
    }

    public Set<ProjectVersionRef> getMissingProjects()
    {
        return new HashSet<ProjectVersionRef>( incompleteSubgraphs );
    }

    public boolean hasVariableProjects()
    {
        return !variableSubgraphs.isEmpty();
    }

    public Set<ProjectVersionRef> getVariableProjects()
    {
        return new HashSet<ProjectVersionRef>( variableSubgraphs );
    }

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

    public Set<EProjectCycle> getCycles()
    {
        return new HashSet<EProjectCycle>( cycles );
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

    public Map<String, String> getProjectMetadata( final ProjectVersionRef ref )
    {
        return metadata.get( ref );
    }

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

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return metadataOwners.get( key );
    }

}
