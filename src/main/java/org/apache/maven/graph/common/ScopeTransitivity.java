package org.apache.maven.graph.common;

public enum ScopeTransitivity
{
    maven
    {
        @Override
        public DependencyScope getChildFor( final DependencyScope scope )
        {
            return DependencyScope.runtime;
        }
    },

    all
    {
        @Override
        public DependencyScope getChildFor( final DependencyScope scope )
        {
            return scope;
        }
    };

    public abstract DependencyScope getChildFor( DependencyScope scope );

}
