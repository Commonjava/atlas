package org.apache.maven.pgraph.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.pgraph.version.part.SeparatorPart;
import org.apache.maven.pgraph.version.part.SnapshotPart;
import org.apache.maven.pgraph.version.part.VersionPart;
import org.apache.maven.pgraph.version.part.VersionPartSeparator;

public class SingleVersion
    implements VersionSpec
{

    private final List<VersionPart<?>> parts;

    public SingleVersion( final VersionPart<?>... parts )
    {
        this( Arrays.asList( parts ) );
    }

    public SingleVersion( final List<VersionPart<?>> p )
    {
        final List<VersionPart<?>> parts = new ArrayList<VersionPart<?>>( p );
        normalize( parts );
        validate( parts );
        this.parts = new ArrayList<VersionPart<?>>( parts );
    }

    private void normalize( final List<VersionPart<?>> parts )
    {
        VersionPart<?> prev = null;
        final List<VersionPart<?>> orig = new ArrayList<VersionPart<?>>( parts );
        for ( int i = 0; i < orig.size(); i++ )
        {
            final VersionPart<?> part = orig.get( i );
            if ( prev != null && !( prev instanceof SeparatorPart ) && !( part instanceof SeparatorPart ) )
            {
                parts.add( i, new SeparatorPart( VersionPartSeparator.BLANK ) );
            }

            prev = part;
        }

        if ( parts.get( parts.size() - 1 ) instanceof SeparatorPart )
        {
            parts.remove( parts.size() - 1 );
        }
    }

    private void validate( final List<VersionPart<?>> parts )
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

        for ( final VersionPart<?> part : parts )
        {
            if ( part != parts.get( parts.size() - 1 ) && part instanceof SnapshotPart )
            {
                throw new IllegalArgumentException( "Snapshot marker MUST appear at the end of the version" );
            }
        }
    }

    public SingleVersion getBaseVersion()
    {
        if ( isRelease() )
        {
            return this;
        }

        return new SingleVersion( parts.subList( 0, parts.size() - 2 ) );
    }

    public String renderStandard()
    {
        return renderStandard( parts );
    }

    private String renderStandard( final List<VersionPart<?>> parts )
    {
        final StringBuilder sb = new StringBuilder();
        for ( final VersionPart<?> part : parts )
        {
            sb.append( part.renderStandard() );
        }

        return sb.toString();
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
        for ( final VersionPart<?> part : parts )
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
        final StringBuilder sb = new StringBuilder();
        sb.append( "SingleVersion: [" );
        for ( final VersionPart<?> part : parts )
        {
            sb.append( part ).append( ", " );
        }
        sb.setLength( sb.length() - 2 );
        sb.append( "] (" ).append( renderStandard() ).append( ")" );

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

    public List<VersionPart<?>> getVersionParts()
    {
        return parts;
    }

}
