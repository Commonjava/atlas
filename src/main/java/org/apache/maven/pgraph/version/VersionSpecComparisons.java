package org.apache.maven.pgraph.version;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.pgraph.version.part.NumericPart;
import org.apache.maven.pgraph.version.part.SeparatorPart;
import org.apache.maven.pgraph.version.part.VersionPart;
import org.apache.maven.pgraph.version.part.VersionPartSeparator;

public final class VersionSpecComparisons
{

    private VersionSpecComparisons()
    {
    }

    public static Comparator<VersionSpec> comparator()
    {
        return new Comparator<VersionSpec>()
        {
            public int compare( final VersionSpec o1, final VersionSpec o2 )
            {
                return compareTo( o1, o2 );
            }
        };
    }

    public static int compareTo( final VersionSpec first, final VersionSpec second )
    {
        if ( first instanceof SingleVersion )
        {
            return compareSingleToSpec( (SingleVersion) first, second );
        }
        else if ( first instanceof RangeVersionSpec )
        {
            return compareRangeToSpec( (RangeVersionSpec) first, second );
        }
        else
        {
            return compareCompoundToSpec( (CompoundVersionSpec) first, second );
        }
    }

    private static int compareCompoundToSpec( final CompoundVersionSpec first, final VersionSpec second )
    {
        if ( second instanceof SingleVersion )
        {
            return -1 * compareSingleToCompound( (SingleVersion) second, first );
        }
        else if ( second instanceof RangeVersionSpec )
        {
            return -1 * compareRangeToCompound( (RangeVersionSpec) second, first );
        }
        else
        {
            return compareCompoundToCompound( first, (CompoundVersionSpec) second );
        }
    }

    private static int compareRangeToSpec( final RangeVersionSpec first, final VersionSpec second )
    {
        if ( second instanceof SingleVersion )
        {
            return -1 * compareSingleToRange( (SingleVersion) second, first );
        }
        else if ( second instanceof RangeVersionSpec )
        {
            return compareRangeToRange( first, (RangeVersionSpec) second );
        }
        else
        {
            return compareRangeToCompound( first, (CompoundVersionSpec) second );
        }
    }

    private static int compareSingleToSpec( final SingleVersion first, final VersionSpec second )
    {
        if ( second instanceof SingleVersion )
        {
            return compareSingleToSingle( first, (SingleVersion) second );
        }
        else if ( second instanceof RangeVersionSpec )
        {
            return compareSingleToRange( first, (RangeVersionSpec) second );
        }
        else
        {
            return compareSingleToCompound( first, (CompoundVersionSpec) second );
        }
    }

    private static int compareCompoundToCompound( final CompoundVersionSpec first, final CompoundVersionSpec second )
    {
        final VersionSpec ff = first.getFirstComponent();
        final VersionSpec sf = second.getFirstComponent();
        final int fc = compareTo( ff, sf );

        final VersionSpec fl = first.getLastComponent();
        final VersionSpec sl = second.getLastComponent();
        final int lc = compareTo( fl, sl );

        final int comp = fc + lc;

        if ( comp == 0 )
        {
            return 0;
        }
        else if ( comp < 0 )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    private static int compareRangeToCompound( final RangeVersionSpec first, final CompoundVersionSpec second )
    {
        final int fc = compareTo( first, second.getFirstComponent() );
        final int lc = compareTo( first, second.getLastComponent() );

        final int comp = fc + lc;

        if ( comp == 0 )
        {
            return 0;
        }
        else if ( comp < 0 )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    private static int compareSingleToCompound( final SingleVersion first, final CompoundVersionSpec second )
    {
        final int fc = compareTo( first, second.getFirstComponent() );
        final int lc = compareTo( first, second.getLastComponent() );

        final int comp = fc + lc;

        if ( comp == 0 )
        {
            return 0;
        }
        else if ( comp < 0 )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    private static int compareRangeToRange( final RangeVersionSpec first, final RangeVersionSpec second )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    private static int compareSingleToRange( final SingleVersion first, final RangeVersionSpec second )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    private static int compareSingleToSingle( final SingleVersion first, final SingleVersion second )
    {
        final SingleVersion firstBase = first.getBaseVersion();
        final SingleVersion secondBase = second.getBaseVersion();

        final int comp = compareTo( firstBase.getVersionParts(), secondBase.getVersionParts() );
        if ( comp == 0 )
        {
            if ( first.isRelease() && !second.isRelease() )
            {
                return 1;
            }
            else if ( second.isRelease() && !first.isRelease() )
            {
                return -1;
            }
        }

        return comp;
    }

    private static int compareTo( final List<VersionPart<?>> first, final List<VersionPart<?>> second )
    {
        final List<VersionPart<?>> fp = new ArrayList<VersionPart<?>>( first );
        final List<VersionPart<?>> sp = new ArrayList<VersionPart<?>>( second );

        final int fLast = first.size() - 1;
        final int sLast = second.size() - 1;
        for ( int i = Math.min( first.size() - 1, second.size() - 1 ); i < Math.max( first.size(), second.size() ); i++ )
        {
            if ( i > fLast )
            {
                fp.add( new SeparatorPart( VersionPartSeparator.BLANK ) );
                fp.add( NumericPart.ZERO );
            }

            if ( i > sLast )
            {
                sp.add( new SeparatorPart( VersionPartSeparator.BLANK ) );
                sp.add( NumericPart.ZERO );
            }
        }

        // the two lists should be of equal length now.
        for ( int i = 0; i < fp.size(); i++ )
        {
            final VersionPart<?> fPart = fp.get( i );
            final VersionPart<?> sPart = sp.get( i );
            final int comp = fPart.compareTo( sPart );
            if ( comp != 0 )
            {
                return comp;
            }
        }

        return 0;
    }

}
