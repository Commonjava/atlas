package org.apache.maven.graph.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum DependencyScope
{
    provided, compile( provided ), runtime( compile ), system, test( provided, compile, runtime, system );

    private final Set<DependencyScope> implied;

    private DependencyScope( final DependencyScope... implied )
    {
        this.implied = new HashSet<DependencyScope>( Arrays.asList( implied ) );
    }

    public boolean implies( final DependencyScope scope )
    {
        return implied.contains( scope );
    }

    public static DependencyScope getScope( final String scope )
    {
        return scope == null ? compile : valueOf( scope );
    }

}
