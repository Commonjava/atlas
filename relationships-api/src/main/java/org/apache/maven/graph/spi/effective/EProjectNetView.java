package org.apache.maven.graph.spi.effective;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.session.EGraphSession;

public class EProjectNetView
{

    public static final EProjectNetView GLOBAL = new EProjectNetView( null );

    private final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

    private final EGraphSession session;

    private final ProjectRelationshipFilter filter;

    public EProjectNetView( final EGraphSession session, final ProjectRelationshipFilter filter,
                            final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.roots.addAll( roots );
        this.session = session;
    }

    public EProjectNetView( final EGraphSession session, final ProjectRelationshipFilter filter,
                            final ProjectVersionRef... roots )
    {
        this.filter = filter;
        this.roots.addAll( Arrays.asList( roots ) );
        this.session = session;
    }

    public EProjectNetView( final EGraphSession session, final Collection<ProjectVersionRef> roots )
    {
        this.filter = null;
        this.roots.addAll( roots );
        this.session = session;
    }

    public EProjectNetView( final EGraphSession session, final ProjectVersionRef... roots )
    {
        this.filter = null;
        this.roots.addAll( Arrays.asList( roots ) );
        this.session = session;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public EGraphSession getSession()
    {
        return session;
    }

}
