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
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.commonjava.maven.atlas.ident.version.part.NumericPart;
import org.commonjava.maven.atlas.ident.version.part.SeparatorPart;
import org.commonjava.maven.atlas.ident.version.part.SnapshotPart;
import org.commonjava.maven.atlas.ident.version.part.StringPart;
import org.commonjava.maven.atlas.ident.version.part.VersionPart;
import org.commonjava.maven.atlas.ident.version.part.VersionPartSeparator;
import org.commonjava.maven.atlas.ident.version.part.VersionPhrase;

public class SingleVersion
    implements VersionSpec, Serializable
{

    private static final long serialVersionUID = 1L;

    private final List<VersionPhrase> phrases;

    private final String rawExpression;

    private SingleVersion()
    {
        phrases = new ArrayList<VersionPhrase>();
        this.rawExpression = "";
    }

    public SingleVersion( final String rawExpression, final VersionPart... parts )
        throws InvalidVersionSpecificationException
    {
        this( rawExpression, Arrays.asList( parts ) );
    }

    public SingleVersion( final String rawExpression, final List<VersionPart> parts )
        throws InvalidVersionSpecificationException
    {
        this.rawExpression = rawExpression;
        phrases = parsePhrases( parts );
        validatePhrases();
    }

    private void validatePhrases()
        throws InvalidVersionSpecificationException
    {
        // NOTE: This is unnecessarily restrictive...the version '0' should be allowed, though it's very strange.
        //        if ( phrases.get( 0 )
        //                    .isSilent() )
        //        {
        //            throw new InvalidVersionSpecificationException( rawExpression, "This version: " + toString()
        //                + " is effectively empty! All parts are 'silent'." );
        //        }
    }

    private List<VersionPhrase> parsePhrases( final List<VersionPart> p )
        throws InvalidVersionSpecificationException
    {
        final List<VersionPart> parts = normalize( p );
        validate( parts );

        final List<VersionPhrase> phrases = new ArrayList<VersionPhrase>();
        VersionPartSeparator currentPhraseSep = VersionPartSeparator.BLANK;
        List<VersionPart> current = new ArrayList<VersionPart>();

        for ( int i = 0; i < parts.size(); i++ )
        {
            final VersionPart prev = i == 0 ? null : parts.get( i - 1 );
            final VersionPart part = parts.get( i );
            final VersionPart next = i >= parts.size() - 1 ? null : parts.get( i + 1 );

            if ( ( part instanceof SeparatorPart ) && ( VersionPartSeparator.DASH == ( (SeparatorPart) part ).getValue() ) )
            {
                if ( prev != null && !( prev instanceof StringPart ) )
                {
                    try
                    {
                        phrases.add( new VersionPhrase( currentPhraseSep, current ) );
                    }
                    catch ( final InvalidVersionSpecificationException e )
                    {
                        // FIXME: Need some way to handle this...
                    }

                    current = new ArrayList<VersionPart>();

                    currentPhraseSep = VersionPartSeparator.DASH;
                }
                else
                {
                    current.add( part );
                }

                if ( next != null )
                {
                    current.add( next );
                    i++;
                }
            }
            else if ( ( ( part instanceof SnapshotPart ) || ( part instanceof StringPart ) ) && prev != null
                && ( !( prev instanceof SeparatorPart ) || ( ( (SeparatorPart) prev ).getValue() != VersionPartSeparator.DASH ) ) )
            {
                VersionPartSeparator sep = null;
                if ( prev instanceof SeparatorPart )
                {
                    sep = ( (SeparatorPart) current.remove( current.size() - 1 ) ).getValue();
                }
                else
                {
                    sep = VersionPartSeparator.BLANK;
                }

                try
                {
                    phrases.add( new VersionPhrase( currentPhraseSep, current ) );
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    // FIXME: Need some way to handle this...
                }

                current = new ArrayList<VersionPart>();
                current.add( part );

                currentPhraseSep = sep;
            }
            else
            {
                current.add( part );
            }
        }

        if ( !current.isEmpty() )
        {
            phrases.add( new VersionPhrase( currentPhraseSep, current ) );
        }

        return phrases;
    }

    private List<VersionPart> normalize( final List<VersionPart> parts )
    {
        VersionPart prev = null;
        final List<VersionPart> result = new ArrayList<VersionPart>( parts.size() );
        for ( int i = 0; i < parts.size(); i++ )
        {
            final VersionPart part = parts.get( i );
            if ( part instanceof SnapshotPart && ( parts.size() == 1 || i < parts.size() - 1 ) )
            {
                final SnapshotPart snap = (SnapshotPart) part;

                if ( snap.isLocalSnapshot() )
                {
                    final StringPart sub = new StringPart( ( (SnapshotPart) part ).getLiteral() );
                    result.add( sub );
                    prev = sub;
                }
                else
                {
                    final StringTokenizer st = new StringTokenizer( snap.getLiteral(), ".-", true );
                    int idx = 0;
                    while ( st.hasMoreTokens() )
                    {
                        final String tok = st.nextToken();
                        if ( idx % 2 == 1 )
                        {
                            final SeparatorPart sep = new SeparatorPart( VersionPartSeparator.find( tok ) );
                            result.add( sep );
                            prev = sep;
                        }
                        else
                        {
                            final NumericPart np = new NumericPart( tok );
                            result.add( np );
                            prev = np;
                        }

                        idx++;
                    }
                }

            }
            else if ( prev != null && !( prev instanceof SeparatorPart ) && !( part instanceof SeparatorPart ) )
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

        if ( result.get( result.size() - 1 ) instanceof SeparatorPart )
        {
            result.remove( result.size() - 1 );
        }

        return result;
    }

    private void validate( final List<VersionPart> parts )
        throws InvalidVersionSpecificationException
    {
        if ( parts.isEmpty() )
        {
            throw new InvalidVersionSpecificationException( rawExpression, "Empty versions are not allowed" );
        }

        if ( parts.size() == 1 && parts.get( 0 ) instanceof SnapshotPart )
        {
            throw new InvalidVersionSpecificationException( rawExpression,
                                                            "Cannot have a version of 'SNAPSHOT'; version must be releasable by dropping the snapshot marker!" );
        }

        for ( final VersionPart part : parts )
        {
            if ( part != parts.get( parts.size() - 1 ) && part instanceof SnapshotPart )
            {
                throw new InvalidVersionSpecificationException( rawExpression, "Snapshot marker MUST appear at the end of the version" );
            }
        }
    }

    public SingleVersion getBaseVersion()
    {
        if ( isRelease() )
        {
            return this;
        }

        final SingleVersion v = new SingleVersion();
        v.phrases.addAll( phrases.subList( 0, phrases.size() - 1 ) );

        return v;
    }

    @Override
    public String renderStandard()
    {
        return rawExpression;
    }

    @Override
    public boolean contains( final VersionSpec version )
    {
        if ( version.isSingle() )
        {
            final SingleVersion sv = version.getSingleVersion();
            if ( !getBaseVersion().equals( sv.getBaseVersion() ) )
            {
                // if my base version doesn't match the other's base version, there's no way I can contain it.
                return false;
            }

            if ( isLocalSnapshot() )
            {
                // if I'm a local snapshot, then I can contain any other single snapshot.
                return version.isSnapshot();
            }
            else if ( sv.isLocalSnapshot() )
            {
                // either I'm a release or a timestamped snapshot.
                // if the other one is a local snapshot, I DON'T contain it.
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public int compareTo( final VersionSpec other )
    {
        return VersionSpecComparisons.compareTo( this, other );
    }

    @Override
    public boolean isRelease()
    {
        return !isSnapshot();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "SingleVersion: [" );
        for ( final VersionPhrase phrase : phrases )
        {
            sb.append( phrase )
              .append( ", " );
        }
        sb.setLength( sb.length() - 2 );
        sb.append( "] (" )
          .append( renderStandard() )
          .append( ")" );

        return sb.toString();
    }

    @Override
    public boolean isSnapshot()
    {
        final VersionPhrase last = phrases.get( phrases.size() - 1 );
        return last.isSnapshot();
    }

    public boolean isLocalSnapshot()
    {
        final VersionPart lastPart = getLastPart();
        if ( lastPart instanceof SnapshotPart )
        {
            final SnapshotPart part = (SnapshotPart) lastPart;
            return part.isLocalSnapshot();
        }

        return false;
    }

    private VersionPart getLastPart()
    {
        int idx = phrases.size();
        VersionPhrase last;
        List<VersionPart> parts;
        do
        {
            idx--;
            last = phrases.get( idx );
            parts = last.getVersionParts();
        }
        while ( idx > 0 && parts.isEmpty() );

        return parts == null || parts.isEmpty() ? null : parts.get( parts.size() - 1 );
    }

    @Override
    public boolean isConcrete()
    {
        return isRelease() || !isLocalSnapshot();
    }

    @Override
    public boolean isSingle()
    {
        return true;
    }

    @Override
    public SingleVersion getConcreteVersion()
    {
        return isConcrete() ? this : null;
    }

    @Override
    public SingleVersion getSingleVersion()
    {
        return isSingle() ? this : null;
    }

    public List<VersionPhrase> getVersionPhrases()
    {
        return phrases;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        if ( phrases != null )
        {
            for ( final VersionPhrase phrase : phrases )
            {
                if ( !phrase.isSilent() )
                {
                    result += phrase.hashCode();
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
        final SingleVersion other = (SingleVersion) obj;
        if ( phrases == null )
        {
            if ( other.phrases != null )
            {
                return false;
            }
        }
        else
        {
            int i = 0;
            for ( ; i < Math.min( phrases.size(), other.phrases.size() ); i++ )
            {
                final VersionPhrase mine = phrases.get( i );
                final VersionPhrase theirs = other.phrases.get( i );
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

            if ( i < phrases.size() )
            {
                for ( int j = i; j < phrases.size(); j++ )
                {
                    final VersionPhrase mine = phrases.get( j );
                    if ( !mine.isSilent() )
                    {
                        return false;
                    }
                }
            }

            if ( i < other.phrases.size() )
            {
                for ( int j = i; j < other.phrases.size(); j++ )
                {
                    final VersionPhrase theirs = other.phrases.get( j );
                    if ( !theirs.isSilent() )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

}
