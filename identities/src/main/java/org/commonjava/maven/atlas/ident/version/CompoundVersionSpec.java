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
package org.commonjava.maven.atlas.ident.version;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CompoundVersionSpec
    implements VersionSpec, Iterable<VersionSpec>, Serializable, MultiVersionSpec
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
            if ( spec.isSingle() )
            {
                final SingleVersion sv = (SingleVersion) spec;
                s.add( new RangeVersionSpec( "[" + spec.renderStandard() + "]", sv, sv, true, true ) );
            }
            else
            {
                s.add( spec );
            }
        }

        Collections.sort( s, VersionSpecComparisons.comparator() );
        this.specs = Collections.unmodifiableList( s );
    }

    public String renderStandard()
    {
        if ( rawExpression == null )
        {
            final StringBuilder sb = new StringBuilder();
            for ( final VersionSpec spec : specs )
            {
                sb.append( spec.renderStandard() );
            }
            return sb.toString();
        }
        else
        {
            return rawExpression;
        }
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

    public boolean isSnapshot()
    {
        for ( final VersionSpec spec : specs )
        {
            if ( spec.isSnapshot() )
            {
                return true;
            }
        }

        return false;
    }

    public boolean isRelease()
    {
        return !isSnapshot();
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

    public boolean isPinned()
    {
        if ( specs.size() != 1 )
        {
            return false;
        }

        final VersionSpec spec = specs.get( 0 );
        if ( spec.isSingle() )
        {
            return true;
        }

        final MultiVersionSpec mvs = (MultiVersionSpec) spec;
        return mvs.isPinned();
    }

    public SingleVersion getPinnedVersion()
    {
        if ( specs.size() != 1 )
        {
            return null;
        }

        final VersionSpec spec = specs.get( 0 );
        if ( spec instanceof SingleVersion )
        {
            return (SingleVersion) spec;
        }

        final MultiVersionSpec mvs = (MultiVersionSpec) spec;
        if ( mvs.isPinned() )
        {
            return mvs.getPinnedVersion();
        }

        return null;
    }

}
