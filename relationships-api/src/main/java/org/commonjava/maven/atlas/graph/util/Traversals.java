package org.commonjava.maven.atlas.graph.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipPathComparator;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

public final class Traversals
{

    private static final Logger logger = new Logger( Traversals.class );

    private Traversals()
    {
    }

    public static void traverse( final GraphView view, final ProjectNetTraversal traversal, final GraphEdgeSelector selector,
                                 final ProjectVersionRef... roots )
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
                    bfsTraverse( view, traversal, i, selector, start );
                    break;
                }
                case depth_first:
                {
                    dfsTraverse( view, traversal, i, selector, start );
                    break;
                }
            }

            traversal.endTraverse( i );
        }
    }

    public static void dfsTraverse( final GraphView view, final ProjectNetTraversal traversal, final int pass, final GraphEdgeSelector selector,
                                    final ProjectVersionRef... roots )
    {
        for ( final ProjectVersionRef root : roots )
        {
            dfsIterate( view, root, traversal, new LinkedList<ProjectRelationship<?>>(), pass, selector );
        }
    }

    private static void dfsIterate( final GraphView view, final ProjectVersionRef node, final ProjectNetTraversal traversal,
                                    final LinkedList<ProjectRelationship<?>> path, final int pass, final GraphEdgeSelector selector )
    {
        final List<ProjectRelationship<?>> edges = selector.getSortedOutEdges( view, node );
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
                            dfsIterate( view, target, traversal, path, pass, selector );
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

    public static void bfsTraverse( final GraphView view, final ProjectNetTraversal traversal, final int pass, final GraphEdgeSelector selector,
                                    final ProjectVersionRef... roots )
    {
        final List<List<ProjectRelationship<?>>> starts = new ArrayList<>();
        for ( final ProjectVersionRef root : roots )
        {
            final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
            path.add( new SelfEdge( root ) );
            starts.add( path );
        }

        bfsIterate( view, starts, traversal, pass, selector );
    }

    private static void bfsIterate( final GraphView view, final List<List<ProjectRelationship<?>>> thisLayer, final ProjectNetTraversal traversal,
                                    final int pass, final GraphEdgeSelector selector )
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

            final List<ProjectRelationship<?>> edges = selector.getSortedOutEdges( view, node );
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
            bfsIterate( view, nextLayer, traversal, pass, selector );
        }
    }

}