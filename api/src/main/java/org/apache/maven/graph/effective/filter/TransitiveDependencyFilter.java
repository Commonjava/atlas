package org.apache.maven.graph.effective.filter;

import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ScopeTransitivity;
import org.apache.maven.graph.common.ref.ProjectRef;

public class TransitiveDependencyFilter
    extends OrFilter
{

    public TransitiveDependencyFilter( final DependencyScope scope )
    {
        super( new OrFilter( new DependencyFilter( scope ), new ParentFilter( false ) ) );
    }

    public TransitiveDependencyFilter( final DependencyScope scope, final Set<ProjectRef> excludes )
    {
        this( scope, ScopeTransitivity.maven, false, true, excludes );
    }

    public TransitiveDependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity,
                                       final boolean includeManaged, final boolean includeConcrete,
                                       final Set<ProjectRef> excludes )
    {
        super(
               new OrFilter(
                             new DependencyFilter( scope, scopeTransitivity, includeManaged, includeConcrete, excludes ),
                             new ParentFilter( false ) ) );
    }

}
