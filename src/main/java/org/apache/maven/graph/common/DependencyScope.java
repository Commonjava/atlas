package org.apache.maven.graph.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum DependencyScope
{
    _import( "import" ), provided, compile( provided ), runtime( compile ), system, test( provided, compile, runtime,
        system );

    private final Set<DependencyScope> implied;

    private String realName;

    private DependencyScope( final String realName, final DependencyScope... implied )
    {
        this.realName = realName;
        this.implied = new HashSet<DependencyScope>( Arrays.asList( implied ) );
    }

    private DependencyScope( final DependencyScope... implied )
    {
        realName = name();
        this.implied = new HashSet<DependencyScope>( Arrays.asList( implied ) );
    }

    public boolean implies( final DependencyScope scope )
    {
        return scope == this || implied.contains( scope );
    }

    public String realName()
    {
        return realName;
    }

    public static DependencyScope getScope( String scope )
    {
        if ( scope == null )
        {
            return null;
        }

        scope = scope.trim()
                     .toLowerCase();

        for ( final DependencyScope ds : values() )
        {
            if ( ds.realName.equals( scope ) )
            {
                return ds;
            }
        }

        return null;
    }

}
