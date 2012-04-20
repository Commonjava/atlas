package org.apache.maven.pgraph.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CompoundVersionSpec
    implements VersionSpec, Iterable<VersionSpec>
{

    private final List<VersionSpec> specs;

    public CompoundVersionSpec( final VersionSpec... specs )
    {
        final List<VersionSpec> s = new ArrayList<VersionSpec>();
        for ( final VersionSpec spec : specs )
        {
            if ( ( spec instanceof SingleVersion ) )
            {
                throw new IllegalArgumentException(
                                                    "Currently concrete versions are NOT supported in compound version specifications." );
            }

            s.add( spec );
        }

        Collections.sort( s, VersionSpecComparisons.comparator() );
        this.specs = Collections.unmodifiableList( s );
    }

    public CompoundVersionSpec( final List<VersionSpec> specs )
    {
        final List<VersionSpec> s = new ArrayList<VersionSpec>();
        for ( final VersionSpec spec : specs )
        {
            if ( ( spec instanceof SingleVersion ) )
            {
                throw new IllegalArgumentException(
                                                    "Currently concrete versions are NOT supported in compound version specifications." );
            }

            s.add( spec );
        }

        Collections.sort( s, VersionSpecComparisons.comparator() );
        this.specs = Collections.unmodifiableList( s );
    }

    public String renderStandard()
    {
        final StringBuilder sb = new StringBuilder();
        for ( final VersionSpec spec : specs )
        {
            sb.append( spec.renderStandard() );
        }

        return sb.toString();
    }

    public boolean contains( final VersionSpec version )
    {
        for ( final VersionSpec spec : specs )
        {
            if ( spec.contains( version ) )
            {
                return true;
            }
        }

        return false;
    }

    public int compareTo( final VersionSpec other )
    {
        return VersionSpecComparisons.compareTo( this, other );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CompoundVersion: [" );
        for ( final VersionSpec spec : specs )
        {
            sb.append( "\n  " ).append( spec );
        }
        sb.append( "\n]\n(" ).append( renderStandard() ).append( ")" );

        return sb.toString();
    }

    public boolean isConcrete()
    {
        return ( specs.size() == 1 && specs.get( 0 ).isConcrete() );
    }

    public Iterator<VersionSpec> iterator()
    {
        return specs.iterator();
    }

    public VersionSpec getFirstComponent()
    {
        return specs.get( 0 );
    }

    public VersionSpec getLastComponent()
    {
        return specs.get( specs.size() - 1 );
    }

    public boolean isSingle()
    {
        return specs.size() == 1 && specs.get( 0 ).isSingle();
    }

    public SingleVersion getConcreteVersion()
    {
        return specs.size() != 1 ? null : specs.get( 0 ).getConcreteVersion();
    }

    public SingleVersion getSingleVersion()
    {
        return specs.size() != 1 ? null : specs.get( 0 ).getSingleVersion();
    }

}
