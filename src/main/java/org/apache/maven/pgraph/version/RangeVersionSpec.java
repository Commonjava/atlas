package org.apache.maven.pgraph.version;

public class RangeVersionSpec
    implements VersionSpec
{

    private final SingleVersion lower;

    private final SingleVersion upper;

    private final boolean lowerInclusive;

    private final boolean upperInclusive;

    private final boolean snapshotsAllowed;

    private final String rawExpression;

    public RangeVersionSpec( final String rawExpression, final SingleVersion lower, final SingleVersion upper,
                             final boolean lowerInclusive, final boolean upperInclusive )
    {
        this.rawExpression = rawExpression;
        if ( lower == null && upper == null )
        {
            throw new IllegalArgumentException(
                                                "You MUST supply at least a lower- or upper-bound version to have a valid range!" );
        }

        this.lower = lower;
        this.upper = upper;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;

        snapshotsAllowed = ( lower != null && !lower.isRelease() ) || ( upper != null && !upper.isRelease() );
    }

    public boolean isPinned()
    {
        return lowerInclusive && upperInclusive && lower != null && lower.equals( upper );
    }

    public boolean isSnapshotsAlowed()
    {
        return snapshotsAllowed;
    }

    public String renderStandard()
    {
        return rawExpression;
    }

    public boolean contains( final VersionSpec version )
    {
        if ( version == null )
        {
            return false;
        }

        if ( version instanceof SingleVersion )
        {
            return containsSingle( (SingleVersion) version );
        }
        else if ( version instanceof RangeVersionSpec )
        {
            return containsRange( (RangeVersionSpec) version );
        }
        else
        {
            for ( final VersionSpec spec : ( (CompoundVersionSpec) version ) )
            {
                if ( !contains( spec ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean containsRange( final RangeVersionSpec version )
    {
        final SingleVersion oLower = version.getLowerBound();
        final SingleVersion oUpper = version.getUpperBound();

        if ( lower != null )
        {
            if ( oLower == null )
            {
                return false;
            }
            else
            {
                final int comp = VersionSpecComparisons.compareTo( lower, version );
                if ( comp > 0 || ( comp == 0 && lowerInclusive && !version.isLowerBoundInclusive() ) )
                {
                    return false;
                }
            }
        }

        if ( upper != null )
        {
            if ( oUpper == null )
            {
                return false;
            }
            else
            {
                final int comp = VersionSpecComparisons.compareTo( upper, version );
                if ( comp < 0 || ( comp == 0 && upperInclusive && !version.isUpperBoundInclusive() ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean containsSingle( final SingleVersion version )
    {
        if ( lower != null )
        {
            if ( lowerInclusive && !lower.isRelease() && !version.isRelease() )
            {
                return true;
            }

            final int comp = VersionSpecComparisons.compareTo( lower, version );
            if ( comp > 0 || ( comp == 0 && !lowerInclusive ) )
            {
                return false;
            }
        }

        if ( upper != null )
        {
            if ( upperInclusive && !upper.isRelease() && !version.isRelease() )
            {
                return true;
            }

            final int comp = VersionSpecComparisons.compareTo( upper, version );
            if ( comp < 0 || ( comp == 0 && !upperInclusive ) )
            {
                return false;
            }
        }

        return true;
    }

    public int compareTo( final VersionSpec other )
    {
        return VersionSpecComparisons.compareTo( this, other );
    }

    @Override
    public String toString()
    {
        return String.format( "Range [lower=%s, lowerInclusive=%s, upper=%s, upperInclusive=%s, snapshots allowed? %s] (%s)",
                              lower, lowerInclusive, upper, upperInclusive, snapshotsAllowed, renderStandard() );
    }

    public SingleVersion getLowerBound()
    {
        return lower;
    }

    public SingleVersion getUpperBound()
    {
        return upper;
    }

    public boolean isLowerBoundInclusive()
    {
        return lowerInclusive;
    }

    public boolean isUpperBoundInclusive()
    {
        return upperInclusive;
    }

    public boolean isConcrete()
    {
        return isPinned() && lower.isConcrete();
    }

    public boolean isSingle()
    {
        return isPinned();
    }

    public SingleVersion getConcreteVersion()
    {
        return isConcrete() ? lower.getConcreteVersion() : null;
    }

    public SingleVersion getSingleVersion()
    {
        return isSingle() ? lower.getSingleVersion() : null;
    }

}
