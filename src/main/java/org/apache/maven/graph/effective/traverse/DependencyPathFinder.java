package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.VersionedProjectRef;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

import edu.uci.ics.jung.graph.DirectedGraph;

public class DependencyPathFinder
{

    private final ArtifactRef target;

    private final DirectedGraph<VersionedProjectRef, ProjectRelationship<?>> graph;

    private final List<List<DependencyRelationship>> paths = new ArrayList<List<DependencyRelationship>>();

    private final VersionedProjectRef root;

    public DependencyPathFinder( final EProjectGraph graph, final ArtifactRef target )
    {
        this.graph = graph.getRawGraph();
        this.root = graph.getRoot();
        this.target = target;
    }

    public synchronized List<List<DependencyRelationship>> getPaths()
    {
        if ( paths.isEmpty() )
        {
            recurseToRoot( target, new ArrayList<DependencyRelationship>() );
        }
        return paths;
    }

    private void recurseToRoot( final VersionedProjectRef declaring, final List<DependencyRelationship> inPath )
    {
        final Collection<ProjectRelationship<?>> edges = graph.getInEdges( target.asVersionedProjectRef() );
        for ( final ProjectRelationship<?> rel : edges )
        {
            if ( rel instanceof DependencyRelationship )
            {
                final VersionedProjectRef decl = rel.getDeclaring();

                final List<DependencyRelationship> currentPath = new ArrayList<DependencyRelationship>( inPath );
                currentPath.add( (DependencyRelationship) rel );
                if ( decl.equals( root ) )
                {
                    paths.add( currentPath );
                }
                else
                {
                    recurseToRoot( decl, currentPath );
                }
            }
        }

    }

}
