/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.apache.maven.graph.common.version;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.graph.common.version.part.NumericPart;
import org.apache.maven.graph.common.version.part.SeparatorPart;
import org.apache.maven.graph.common.version.part.VersionPart;
import org.apache.maven.graph.common.version.part.VersionPhrase;

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
        final SingleVersion flb = first.getLowerBound();
        final SingleVersion slb = second.getLowerBound();
        if ( flb == null && slb == null )
        {
            return 0;
        }
        else if ( flb == null && slb != null )
        {
            return -1;
        }
        else if ( slb == null && flb != null )
        {
            return 1;
        }
        else if ( flb != null && slb != null )
        {
            final int comp = compareSingleToSingle( flb, slb );
            if ( comp != 0 )
            {
                return comp;
            }

            final boolean flbi = first.isLowerBoundInclusive();
            final boolean slbi = second.isLowerBoundInclusive();
            if ( !flbi && slbi )
            {
                return -1;
            }
            else if ( flbi && !slbi )
            {
                return 1;
            }

            return 0;
        }
        else
        {
            final SingleVersion fub = first.getUpperBound();
            final SingleVersion sub = second.getUpperBound();
            if ( fub != null && sub == null )
            {
                return -1;
            }
            else if ( sub != null && fub == null )
            {
                return 1;
            }
            else if ( fub != null && sub != null )
            {
                final int comp = compareSingleToSingle( fub, sub );
                if ( comp != 0 )
                {
                    return comp;
                }

                final boolean fubi = first.isUpperBoundInclusive();
                final boolean subi = second.isUpperBoundInclusive();
                if ( !fubi && subi )
                {
                    return -1;
                }
                else if ( fubi && !subi )
                {
                    return 1;
                }

                return 0;
            }
        }

        // punt.
        // How do we get here?
        // 1. lower bounds are equal, and neither has upper bound specified.
        // ...... For sorting DOWN, it's not clear how these two compare.
        return 0;
    }

    private static int compareSingleToRange( final SingleVersion first, final RangeVersionSpec second )
    {
        final SingleVersion lb = second.getLowerBound();
        if ( lb == null )
        {
            final SingleVersion ub = second.getUpperBound();
            if ( ub == null )
            {
                // SHOULD NEVER HAPPEN. THIS IS NOT A VALID RANGE, IT WOULD BE
                // THE EQUIV OF 'ANY'
                return 0;
            }
            else
            {
                final int comp = compareSingleToSingle( first, ub );
                if ( comp != 0 )
                {
                    // FIXME: If upper bound is non-inclusive, the MAY be wrong!
                    return comp;
                }

                if ( second.isUpperBoundInclusive() )
                {
                    return 0;
                }
                else
                {
                    return 1;
                }
            }
        }
        else
        {
            final int comp = compareSingleToSingle( first, lb );
            if ( comp != 0 )
            {
                // FIXME: If lower bound is non-inclusive, the MAY be wrong!
                return comp;
            }

            if ( second.isLowerBoundInclusive() )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
    }

    private static int compareSingleToSingle( final SingleVersion first, final SingleVersion second )
    {
        final int comp = comparePhrasesToPhrases( first.getVersionPhrases(), second.getVersionPhrases() );

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

    private static int comparePhrasesToPhrases( final List<VersionPhrase> firstPhrases,
                                                final List<VersionPhrase> secondPhrases )
    {
        final List<VersionPhrase> fp = new ArrayList<VersionPhrase>( firstPhrases );
        final List<VersionPhrase> sp = new ArrayList<VersionPhrase>( secondPhrases );

        int comp = 0;
        for ( int i = Math.min( firstPhrases.size() - 1, secondPhrases.size() - 1 ); i < Math.max( firstPhrases.size(),
                                                                                                   secondPhrases.size() ); i++ )
        {
            final VersionPhrase f = i < firstPhrases.size() ? firstPhrases.get( i ) : null;
            final VersionPhrase s = i < secondPhrases.size() ? secondPhrases.get( i ) : null;

            try
            {
                if ( f == null )
                {
                    fp.add( new VersionPhrase( s.getSeparator(), NumericPart.ZERO ) );
                }
                else if ( s == null )
                {
                    sp.add( new VersionPhrase( f.getSeparator(), NumericPart.ZERO ) );
                }
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                // FIXME: Some sort of handling...should never happen, but still.
            }
        }

        for ( int i = 0; i < fp.size(); i++ )
        {
            final VersionPhrase f = fp.get( i );
            final VersionPhrase s = sp.get( i );

            comp = comparePhraseToPhrase( f, s );
            if ( comp != 0 )
            {
                return comp;
            }
        }

        return 0;
    }

    public static int comparePhraseToPhrase( final VersionPhrase first, final VersionPhrase second )
    {
        Integer fmi = first.getMarkerIndex();
        Integer smi = second.getMarkerIndex();

        if ( fmi == null )
        {
            fmi = 0;
        }

        if ( smi == null )
        {
            smi = 0;
        }

        int comp = fmi.compareTo( smi );

        if ( comp != 0 )
        {
            return comp;
        }
        comp = comparePartsToParts( first.getVersionParts(), second.getVersionParts() );

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

    private static int comparePartsToParts( final List<VersionPart> first, final List<VersionPart> second )
    {
        final List<VersionPart> fp = new ArrayList<VersionPart>( first );
        final List<VersionPart> sp = new ArrayList<VersionPart>( second );

        for ( int i = Math.min( first.size() - 1, second.size() - 1 ); i < Math.max( first.size(), second.size() ); i++ )
        {
            final VersionPart f = i < first.size() ? first.get( i ) : null;
            final VersionPart s = i < second.size() ? second.get( i ) : null;
            if ( f == null )
            {
                if ( s instanceof SeparatorPart )
                {
                    fp.add( s );
                }
                else
                {
                    fp.add( NumericPart.ZERO );
                }
            }
            else if ( s == null )
            {
                if ( f instanceof SeparatorPart )
                {
                    sp.add( f );
                }
                else
                {
                    sp.add( NumericPart.ZERO );
                }
            }
        }

        // the two lists should be of equal length now.
        for ( int i = 0; i < fp.size(); i++ )
        {
            final VersionPart fPart = fp.get( i );
            final VersionPart sPart = sp.get( i );
            final int comp = fPart.compareTo( sPart );
            if ( comp != 0 )
            {
                return comp;
            }
        }

        return 0;
    }

}
