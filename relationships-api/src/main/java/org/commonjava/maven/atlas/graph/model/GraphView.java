package org.commonjava.maven.atlas.graph.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphView
{

    public static final GraphView GLOBAL = new GraphView( null );

    private final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

    private final GraphWorkspace workspace;

    private final ProjectRelationshipFilter filter;

    public GraphView( final GraphWorkspace workspace, final ProjectRelationshipFilter filter,
                            final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots.addAll( roots );
        this.workspace = workspace;
    }

    public GraphView( final GraphWorkspace workspace, final ProjectRelationshipFilter filter,
                            final ProjectVersionRef... roots )
    {
        this.filter = filter;
        this.roots.addAll( Arrays.asList( roots ) );
        this.workspace = workspace;
    }

    public GraphView( final GraphWorkspace workspace, final Collection<ProjectVersionRef> roots )
    {
        this.filter = null;
        this.roots.addAll( roots );
        this.workspace = workspace;
    }

    public GraphView( final GraphWorkspace workspace, final ProjectVersionRef... roots )
    {
        this.filter = null;
        this.roots.addAll( Arrays.asList( roots ) );
        this.workspace = workspace;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public GraphWorkspace getWorkspace()
    {
        return workspace;
    }

}
