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
package org.commonjava.atlas.maven.ident.version.part;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.commonjava.atlas.maven.ident.version.InvalidVersionSpecificationException;
import org.commonjava.atlas.maven.ident.version.VersionSpecComparisons;

public class VersionPhrase
    implements Comparable<VersionPhrase>, Serializable
{

    private static final long serialVersionUID = 1L;

    private final List<VersionPart> parts;

    private final VersionPartSeparator separator;

    private final Integer markerIndex;

    private boolean silent = false;

    public VersionPhrase( final VersionPartSeparator separator, final VersionPart... parts )
        throws InvalidVersionSpecificationException
    {
        this( separator, Arrays.asList( parts ) );
    }

    public VersionPhrase( final VersionPartSeparator separator, final List<VersionPart> p )
        throws InvalidVersionSpecificationException
    {
        this.separator = separator;
        List<VersionPart> parts = new ArrayList<VersionPart>( p );
        parts = normalize( parts );
        validate( parts );
        markSilentParts( parts );
        this.markerIndex = findMarkerIndex( parts.get( 0 ) );

        this.parts = new ArrayList<VersionPart>( parts );
    }

    private void markSilentParts( final List<VersionPart> parts )
    {
        for ( int i = parts.size() - 1; i > -1; i-- )
        {
            final VersionPart part = parts.get( i );
            boolean silenced = false;
            if ( NumericPart.ZERO.equals( part ) )
            {
                silenced = true;
            }
            else if ( part instanceof SeparatorPart )
            {
                silenced = true;
            }
            else if ( ( part instanceof StringPart )
                && ( (StringPart) part ).getZeroCompareIndex() == StringPart.ADJ_ZERO_EQUIV_INDEX )
            {
                silenced = true;
            }

            if ( silenced )
            {
                part.setSilent( true );
            }
            else
            {
                break;
            }
        }

        if ( parts.get( 0 )
                  .isSilent() )
        {
            silent = true;
        }
    }

    public boolean isSilent()
    {
        return silent;
    }

    private Integer findMarkerIndex( final VersionPart part )
    {
        Integer markerIndex = null;

        if ( part instanceof StringPart )
        {
            markerIndex = ( (StringPart) part ).getZeroCompareIndex();
        }
        else if ( part instanceof SnapshotPart )
        {
            markerIndex = StringPart.ADJ_ZERO_EQUIV_INDEX;
        }
        else if ( ( part instanceof NumericPart ) && ( ( (NumericPart) part ).equals( NumericPart.ZERO ) ) )
        {
            markerIndex = StringPart.ADJ_ZERO_EQUIV_INDEX;
        }

        return markerIndex;
    }

    public VersionPartSeparator getSeparator()
    {
        return separator;
    }

    public Integer getMarkerIndex()
    {
        return markerIndex;
    }

    private List<VersionPart> normalize( final List<VersionPart> parts )
    {
        VersionPart prev = null;
        final List<VersionPart> result = new ArrayList<VersionPart>( parts.size() );
        for ( int i = 0; i < parts.size(); i++ )
        {
            final VersionPart part = parts.get( i );
            if ( prev != null && !( prev instanceof SeparatorPart ) && !( part instanceof SeparatorPart ) )
            {
                final SeparatorPart sep = new SeparatorPart( VersionPartSeparator.BLANK );
                result.add( sep );
                prev = sep;
                i--;
            }
            else
            {
                result.add( part );
                prev = part;
            }
        }

        if ( !result.isEmpty() )
        {
            if ( result.get( result.size() - 1 ) instanceof SeparatorPart )
            {
                result.remove( result.size() - 1 );
            }
        }

        return result;
    }

    private void validate( final List<VersionPart> parts )
        throws InvalidVersionSpecificationException
    {
        if ( parts.isEmpty() )
        {
            throw new InvalidVersionSpecificationException( renderStandard( parts ), "Empty versions are not allowed" );
        }

        for ( final VersionPart part : parts )
        {
            if ( part != parts.get( parts.size() - 1 ) && part instanceof SnapshotPart )
            {
                throw new InvalidVersionSpecificationException( renderStandard( parts ),
                                                                "Snapshot marker MUST appear at the end of the version" );
            }
        }
    }

    public VersionPhrase getBaseVersion()
        throws InvalidVersionSpecificationException
    {
        if ( isRelease() )
        {
            return this;
        }

        try
        {
            return new VersionPhrase( separator, parts.subList( 0, parts.size() - 2 ) );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new InvalidVersionSpecificationException(
                                                            e.getVersion(),
                                                            "Cannot create base version for non-release: %s. Reason: %s",
                                                            e, e.getVersion(), e.getMessage() );
        }
    }

    public String renderDebug()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "PHRASE[sep=" )
          .append( separator == null ? "NULL" : separator.getRenderedString() )
          .append( ", parts=(" );
        for ( final VersionPart part : parts )
        {
            sb.append( part )
              .append( ", " );
        }
        sb.setLength( sb.length() - 2 );
        sb.append( ")] (" )
          .append( renderStandard() )
          .append( ")" );

        return sb.toString();
    }

    public String renderStandard()
    {
        return ( separator == null ? "" : separator.getRenderedString() ) + renderStandard( parts );
    }

    private String renderStandard( final List<VersionPart> parts )
    {
        final StringBuilder sb = new StringBuilder();
        for ( final VersionPart part : parts )
        {
            sb.append( part.renderStandard() );
        }

        return sb.toString();
    }

    @Override
    public int compareTo( final VersionPhrase other )
    {
        return VersionSpecComparisons.comparePhraseToPhrase( this, other );
    }

    public boolean isRelease()
    {
        for ( final VersionPart part : parts )
        {
            if ( part instanceof SnapshotPart )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString()
    {
        return renderStandard();
    }

    public boolean isConcrete()
    {
        return isRelease();
    }

    public boolean isSingle()
    {
        return true;
    }

    public VersionPhrase getConcreteVersion()
    {
        return isConcrete() ? this : null;
    }

    public VersionPhrase getSingleVersion()
    {
        return isSingle() ? this : null;
    }

    public List<VersionPart> getVersionParts()
    {
        return parts;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        if ( parts != null )
        {
            for ( final VersionPart part : parts )
            {
                if ( !part.isSilent() )
                {
                    result += part.hashCode();
                }
            }
        }

        return prime * result;
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
        final VersionPhrase other = (VersionPhrase) obj;
        if ( parts == null )
        {
            if ( other.parts != null )
            {
                return false;
            }
        }
        else
        {
            final List<VersionPart> myParts = new ArrayList<VersionPart>( parts );
            for ( final Iterator<VersionPart> it = myParts.iterator(); it.hasNext(); )
            {
                final VersionPart part = it.next();
                if ( part instanceof SeparatorPart )
                {
                    it.remove();
                }
            }

            final List<VersionPart> theirParts = new ArrayList<VersionPart>( other.parts );
            for ( final Iterator<VersionPart> it = theirParts.iterator(); it.hasNext(); )
            {
                final VersionPart part = it.next();
                if ( part instanceof SeparatorPart )
                {
                    it.remove();
                }
            }

            int i = 0;
            for ( ; i < Math.min( myParts.size(), theirParts.size() ); i++ )
            {
                final VersionPart mine = myParts.get( i );
                final VersionPart theirs = theirParts.get( i );
                if ( mine.isSilent() != theirs.isSilent() )
                {
                    return false;
                }
                else if ( !mine.isSilent() && !theirs.isSilent() )
                {
                    if ( !mine.equals( theirs ) )
                    {
                        return false;
                    }
                }
            }

            if ( i < myParts.size() )
            {
                for ( int j = i; j < myParts.size(); j++ )
                {
                    final VersionPart mine = myParts.get( j );
                    if ( !mine.isSilent() )
                    {
                        return false;
                    }
                }
            }

            if ( i < theirParts.size() )
            {
                for ( int j = i; j < theirParts.size(); j++ )
                {
                    final VersionPart theirs = theirParts.get( j );

                    if ( !theirs.isSilent() )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isSnapshotOnly()
    {
        return parts.size() == 1 && isSnapshot();
    }

    public boolean isSnapshot()
    {
        return parts.get( parts.size() - 1 ) instanceof SnapshotPart;
    }

}
