/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.ident.version;

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
