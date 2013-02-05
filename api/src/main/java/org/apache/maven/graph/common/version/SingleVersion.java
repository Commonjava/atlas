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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.graph.common.version.part.NumericPart;
import org.apache.maven.graph.common.version.part.SeparatorPart;
import org.apache.maven.graph.common.version.part.SnapshotPart;
import org.apache.maven.graph.common.version.part.StringPart;
import org.apache.maven.graph.common.version.part.VersionPart;
import org.apache.maven.graph.common.version.part.VersionPartSeparator;
import org.apache.maven.graph.common.version.part.VersionPhrase;

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
        if ( phrases.get( 0 )
                    .isSilent() )
        {
            throw new InvalidVersionSpecificationException( rawExpression, "This version: " + toString()
                + " is effectively empty! All parts are 'silent'." );
        }
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

            if ( ( part instanceof SeparatorPart )
                && ( VersionPartSeparator.DASH == ( (SeparatorPart) part ).getValue() ) )
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
            else if ( ( ( part instanceof SnapshotPart ) || ( part instanceof StringPart ) )
                && prev != null
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
                throw new InvalidVersionSpecificationException( rawExpression,
                                                                "Snapshot marker MUST appear at the end of the version" );
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

    public String renderStandard()
    {
        return rawExpression;
    }

    public boolean contains( final VersionSpec version )
    {
        return version.isSingle() && equals( version.getSingleVersion() );
    }

    public int compareTo( final VersionSpec other )
    {
        return VersionSpecComparisons.compareTo( this, other );
    }

    public boolean isRelease()
    {
        final VersionPhrase last = phrases.get( phrases.size() - 1 );
        return !last.isSnapshot();
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

    public boolean isSnapshot()
    {
        return !isRelease();
    }

    public boolean isConcrete()
    {
        return isRelease();
    }

    public boolean isSingle()
    {
        return true;
    }

    public SingleVersion getConcreteVersion()
    {
        return isConcrete() ? this : null;
    }

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
