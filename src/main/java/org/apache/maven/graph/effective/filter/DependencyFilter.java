package org.apache.maven.graph.effective.filter;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ScopeTransitivity;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class DependencyFilter
    implements ProjectRelationshipFilter
{

    private final DependencyScope scope;

    private ScopeTransitivity scopeTransitivity = ScopeTransitivity.maven;

    private boolean includeManaged = false;

    private boolean includeConcrete = true;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    public DependencyFilter()
    {
        this.scope = null;
    }

    public DependencyFilter( final DependencyScope scope )
    {
        this.scope = scope;
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity,
                             final boolean includeManaged, final boolean includeConcrete, final Set<ProjectRef> excludes )
    {
        this.scope = scope;
        this.scopeTransitivity = scopeTransitivity;
        this.includeConcrete = includeConcrete;
        this.includeManaged = includeManaged;
        if ( excludes != null )
        {
            this.excludes.addAll( excludes );
        }
    }

    public DependencyFilter( final DependencyFilter parent, final DependencyRelationship parentRel )
    {
        this.scopeTransitivity = parent.scopeTransitivity;
        this.includeConcrete = parent.includeConcrete;
        this.includeManaged = parent.includeManaged;
        this.scope = parent.scope == null ? parent.scope : scopeTransitivity.getChildFor( parent.scope );

        if ( parent.excludes != null )
        {
            excludes.addAll( parent.excludes );
        }

        final Set<ProjectRef> excl = parentRel.getExcludes();
        if ( excl != null && !excl.isEmpty() )
        {
            excludes.addAll( excl );
        }
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        if ( rel instanceof DependencyRelationship )
        {
            final DependencyRelationship dr = (DependencyRelationship) rel;
            if ( excludes.contains( dr.getTarget()
                                      .asProjectRef() ) )
            {
                return false;
            }

            if ( scope != null )
            {
                if ( !scope.implies( dr.getScope() ) )
                {
                    return false;
                }
            }

            if ( !includeManaged && dr.isManaged() )
            {
                return false;
            }
            else if ( !includeConcrete && !dr.isManaged() )
            {
                return false;
            }

            return true;
        }

        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        if ( !( parent instanceof DependencyRelationship ) )
        {
            throw new IllegalArgumentException(
                                                "You can only create a child DependencyFilter if the parent relationship was a DependencyRelationship." );
        }

        return new DependencyFilter( this, (DependencyRelationship) parent );
    }

}
