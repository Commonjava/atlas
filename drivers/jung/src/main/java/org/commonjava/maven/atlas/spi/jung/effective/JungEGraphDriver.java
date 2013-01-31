package org.commonjava.maven.atlas.spi.jung.effective;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.VersionSpec;
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

    private final DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();

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

        if ( !graph.containsVertex( rel.getTarget()
                                       .asProjectVersionRef() ) )
        {
            graph.addVertex( rel.getTarget()
                                .asProjectVersionRef() );
            changed = true;
        }

        if ( !graph.containsEdge( rel ) )
        {
            graph.addEdge( rel, rel.getDeclaring(), rel.getTarget()
                                                       .asProjectVersionRef() );
            changed = true;
        }

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

    public void restrictProjectMembership( final Set<ProjectVersionRef> refs )
    {
        // TODO Auto-generated method stub

        throw new UnsupportedOperationException( "Not implemented!" );
    }

    public void restrictRelationshipMembership( final Set<ProjectRelationship<?>> rels )
    {
        // TODO Auto-generated method stub

        throw new UnsupportedOperationException( "Not implemented!" );
    }

    public void selectVersion( final ProjectVersionRef ref, final VersionSpec spec )
    {
        // TODO Auto-generated method stub

        throw new UnsupportedOperationException( "Not implemented!" );
    }

    public Set<ProjectVersionRef> getUnconnectedProjectReferences()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException( "Not implemented!" );
    }

    public Set<ProjectVersionRef> getVariableProjectReferences()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException( "Not implemented!" );
    }

    public void close()
        throws IOException
    {
        // TODO Save graph somehow??
    }
}
