package org.apache.maven.graph.effective.session;

import static org.apache.commons.lang.StringUtils.join;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;

public class EGraphSession
    implements Closeable
{

    private final Set<URI> activePomLocations;

    private final Set<URI> activeSources;

    private final Map<ProjectVersionRef, SingleVersion> selectedVersions =
        new HashMap<ProjectVersionRef, SingleVersion>();

    private final Set<EGraphSessionListener> listeners = new HashSet<EGraphSessionListener>();

    private boolean open;

    private EGraphSession( final Set<URI> activePomLocations, final Set<URI> activeSources )
    {
        this.activePomLocations = activePomLocations;
        this.activeSources = activeSources;
    }

    public synchronized void addListener( final EGraphSessionListener listener )
    {
        checkOpen();
        listeners.add( listener );
    }

    public synchronized void removeListener( final EGraphSessionListener listener )
    {
        checkOpen();
        listeners.remove( listener );
    }

    public synchronized void clearListeners()
    {
        checkOpen();
        listeners.clear();
    }

    public Iterable<URI> activePomLocations()
    {
        return activePomLocations;
    }

    public Iterable<URI> activeSources()
    {
        return activeSources;
    }

    public ProjectVersionRef selectVersion( final ProjectVersionRef ref, final SingleVersion version )
    {
        checkOpen();
        if ( !version.isConcrete() )
        {
            throw new IllegalArgumentException( "You cannot select a non-concrete version!" );
        }

        final SingleVersion old = selectedVersions.put( ref, version );
        if ( old == null || !old.equals( version ) )
        {
            fireEvent( new SessionSelectionAddEvent( ref, version, this ) );
        }

        final ProjectVersionRef updated = ref.selectVersion( version );
        return updated;
    }

    private void fireEvent( final EGraphSessionEvent event )
    {
        for ( final EGraphSessionListener listener : listeners )
        {
            listener.onEGraphSessionEvent( event );
        }
    }

    public Map<ProjectVersionRef, SingleVersion> clearVersionSelections()
    {
        checkOpen();
        final Map<ProjectVersionRef, SingleVersion> old =
            new HashMap<ProjectVersionRef, SingleVersion>( selectedVersions );

        selectedVersions.clear();
        fireEvent( new SessionSelectionClearEvent( this ) );

        return old;
    }

    public SingleVersion getSelectedVersion( final ProjectVersionRef ref )
    {
        checkOpen();
        return selectedVersions.get( ref.asProjectVersionRef() );
    }

    public Map<ProjectVersionRef, SingleVersion> getVersionSelections()
    {
        return selectedVersions;
    }

    public static final class Builder
    {
        private final Set<URI> activePomLocations = new LinkedHashSet<URI>();

        private final Set<URI> activeSources = new LinkedHashSet<URI>();

        public Builder withPomLocations( final URI... pomLocations )
        {
            this.activePomLocations.addAll( Arrays.asList( pomLocations ) );
            return this;
        }

        public Builder withPomLocations( final Collection<URI> pomLocations )
        {
            this.activePomLocations.addAll( pomLocations );
            return this;
        }

        public Builder withPomLocation( final URI pomLocation )
        {
            this.activePomLocations.add( pomLocation );
            return this;
        }

        public Builder withSources( final URI... sources )
        {
            this.activeSources.addAll( Arrays.asList( sources ) );
            return this;
        }

        public Builder withSources( final Collection<URI> sources )
        {
            this.activeSources.addAll( sources );
            return this;
        }

        public Builder withSource( final URI source )
        {
            this.activeSources.add( source );
            return this;
        }

        public EGraphSession build()
        {
            return new EGraphSession( Collections.unmodifiableSet( activePomLocations ),
                                      Collections.unmodifiableSet( activeSources ) );
        }
    }

    @Override
    public String toString()
    {
        return String.format( "EGraphSession (activePomLocations=[%s], activeSources=[%s])",
                              join( activePomLocations, ", " ), join( activeSources, ", " ) );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result =
            prime * result + ( ( activePomLocations == null ) ? 0 : new HashSet<URI>( activePomLocations ).hashCode() );
        result = prime * result + ( ( activeSources == null ) ? 0 : new HashSet<URI>( activeSources ).hashCode() );
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
        final EGraphSession other = (EGraphSession) obj;
        if ( activePomLocations == null )
        {
            if ( other.activePomLocations != null )
            {
                return false;
            }
        }
        else if ( !new HashSet<URI>( activePomLocations ).equals( new HashSet<URI>( other.activePomLocations ) ) )
        {
            return false;
        }
        if ( activeSources == null )
        {
            if ( other.activeSources != null )
            {
                return false;
            }
        }
        else if ( !new HashSet<URI>( activeSources ).equals( new HashSet<URI>( other.activeSources ) ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        if ( open )
        {
            clearVersionSelections();
            fireEvent( new SessionCloseEvent( this ) );
            clearListeners();
            open = false;
        }
    }

    private synchronized void checkOpen()
    {
        if ( !open )
        {
            throw new IllegalStateException( "EGraphSession instance is closed! You can only use open sessions." );
        }
    }

}
