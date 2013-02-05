/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.common.version;

import java.io.Serializable;

public class RangeVersionSpec
    implements VersionSpec, Serializable, MultiVersionSpec
{

    private static final long serialVersionUID = 1L;

    private final SingleVersion lower;

    private final SingleVersion upper;

    private final boolean lowerInclusive;

    private final boolean upperInclusive;

    private final boolean snapshotAllowed;

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

        snapshotAllowed = ( lower != null && !lower.isRelease() ) || ( upper != null && !upper.isRelease() );
    }

    public boolean isPinned()
    {
        return lowerInclusive && upperInclusive && lower != null && lower.equals( upper );
    }

    public SingleVersion getPinnedVersion()
    {
        return isPinned() ? lower : null;
    }

    public boolean isSnapshot()
    {
        return snapshotAllowed;
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
                              lower, lowerInclusive, upper, upperInclusive, snapshotAllowed, renderStandard() );
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( lower == null ) ? 0 : lower.hashCode() );
        result = prime * result + ( lowerInclusive ? 1231 : 1237 );
        result = prime * result + ( snapshotAllowed ? 1231 : 1237 );
        result = prime * result + ( ( upper == null ) ? 0 : upper.hashCode() );
        result = prime * result + ( upperInclusive ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final RangeVersionSpec other = (RangeVersionSpec) obj;
        if ( lower == null )
        {
            if ( other.lower != null )
            {
                return false;
            }
        }
        else if ( !lower.equals( other.lower ) )
        {
            return false;
        }
        if ( lowerInclusive != other.lowerInclusive )
        {
            return false;
        }
        if ( snapshotAllowed != other.snapshotAllowed )
        {
            return false;
        }
        if ( upper == null )
        {
            if ( other.upper != null )
            {
                return false;
            }
        }
        else if ( !upper.equals( other.upper ) )
        {
            return false;
        }
        if ( upperInclusive != other.upperInclusive )
        {
            return false;
        }
        return true;
    }

}
