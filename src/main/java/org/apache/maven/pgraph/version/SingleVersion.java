package org.apache.maven.pgraph.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.pgraph.version.part.NumericPart;
import org.apache.maven.pgraph.version.part.SeparatorPart;
import org.apache.maven.pgraph.version.part.SnapshotPart;
import org.apache.maven.pgraph.version.part.StringPart;
import org.apache.maven.pgraph.version.part.VersionPart;

public class SingleVersion
    implements VersionSpec
{

    private final List<VersionPart> parts;

    private final String rawExpression;

    private SingleVersion()
    {
        parts = new ArrayList<VersionPart>();
        this.rawExpression = "";
    }

    public SingleVersion( final String rawExpression, final VersionPart... parts )
    {
        this( rawExpression, Arrays.asList( parts ) );
    }

    public SingleVersion( final String rawExpression, final List<VersionPart> parts )
    {
        this.rawExpression = rawExpression;
        this.parts = parts;
        normalize( this.parts );
        validate( this.parts );
    }

    public static boolean isSilentPart( final VersionPart part )
    {
        if ( NumericPart.ZERO.equals( part ) )
        {
            return true;
        }
        else if ( part instanceof SeparatorPart )
        {
            return true;
        }
        else if ( ( part instanceof StringPart )
            && ( (StringPart) part ).getZeroCompareIndex() == StringPart.ADJ_ZERO_EQUIV_INDEX )
        {
            return true;
        }

        return false;
    }

    private void normalize( final List<VersionPart> parts )
    {
        for ( int i = parts.size() - 1; i > -1; i-- )
        {
            final VersionPart part = parts.get( i );
            if ( isSilentPart( part ) )
            {
                parts.remove( i );
            }
        }

        if ( parts.get( parts.size() - 1 ) instanceof SeparatorPart )
        {
            parts.remove( parts.size() - 1 );
        }

        // return result;
    }

    private void validate( final List<VersionPart> parts )
    {
        if ( parts.isEmpty() )
        {
            throw new IllegalArgumentException( "Empty versions are not allowed" );
        }

        if ( parts.size() == 1 && parts.get( 0 ) instanceof SnapshotPart )
        {
            throw new IllegalArgumentException(
                                                "Cannot have a version of 'SNAPSHOT'; version must be releasable by dropping the snapshot marker!" );
        }

        for ( final VersionPart part : parts )
        {
            if ( part != parts.get( parts.size() - 1 ) && part instanceof SnapshotPart )
            {
                throw new IllegalArgumentException( "Snapshot marker MUST appear at the end of the version" );
            }
        }
    }

    public List<VersionPart> getVersionParts()
    {
        return parts;
    }

    public SingleVersion getBaseVersion()
    {
        if ( isRelease() )
        {
            return this;
        }

        final SingleVersion v = new SingleVersion();
        v.parts.addAll( parts.subList( 0, parts.size() - 2 ) );

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
        final VersionPart last = parts.get( parts.size() - 1 );
        return !( last instanceof SnapshotPart );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "SingleVersion: [" );
        sb.append( renderStandard( parts ) );
        sb.append( "] (" )
          .append( renderStandard() )
          .append( ")" );

        return sb.toString();
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        if ( parts != null )
        {
            for ( final VersionPart part : parts )
            {
                result += part.hashCode();
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

            if ( myParts.size() != theirParts.size() )
            {
                return false;
            }

            for ( int i = 0; i < myParts.size(); i++ )
            {
                final VersionPart mine = myParts.get( i );
                final VersionPart theirs = theirParts.get( i );
                if ( !mine.equals( theirs ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

}
