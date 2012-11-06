package org.apache.maven.graph.common.version;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CompoundVersionSpec
    implements VersionSpec, Iterable<VersionSpec>, Serializable
{

    private static final long serialVersionUID = 1L;

    private final List<VersionSpec> specs;

    private final String rawExpression;

    public CompoundVersionSpec( final String rawExpression, final VersionSpec... specs )
    {
        this.rawExpression = rawExpression;
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

    public CompoundVersionSpec( final String rawExpression, final List<VersionSpec> specs )
    {
        this.rawExpression = rawExpression;
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
        return rawExpression;
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
            sb.append( "\n  " )
              .append( spec );
        }
        sb.append( "\n]\n(" )
          .append( renderStandard() )
          .append( ")" );

        return sb.toString();
    }

    public boolean isConcrete()
    {
        return ( specs.size() == 1 && specs.get( 0 )
                                           .isConcrete() );
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
        return specs.size() == 1 && specs.get( 0 )
                                         .isSingle();
    }

    public SingleVersion getConcreteVersion()
    {
        return specs.size() != 1 ? null : specs.get( 0 )
                                               .getConcreteVersion();
    }

    public SingleVersion getSingleVersion()
    {
        return specs.size() != 1 ? null : specs.get( 0 )
                                               .getSingleVersion();
    }

    public int getComponentCount()
    {
        return specs.size();
    }

}
