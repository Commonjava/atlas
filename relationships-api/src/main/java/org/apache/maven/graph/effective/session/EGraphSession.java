package org.apache.maven.graph.effective.session;

import static org.apache.commons.lang.StringUtils.join;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.spi.GraphDriverException;

public abstract class EGraphSession
{

    private final Set<URI> activePomLocations;

    private final Set<URI> activeSources;

    private final Map<ProjectVersionRef, SingleVersion> selectedVersions =
        new HashMap<ProjectVersionRef, SingleVersion>();

    private final Map<String, Object> properties = new HashMap<String, Object>();

    private final String id;

    private boolean open = true;

    private final List<GraphSessionListener> listeners = new ArrayList<GraphSessionListener>();

    protected EGraphSession( final String id, final EGraphSessionConfiguration config )
    {
        this.id = id;
        this.activePomLocations = config.getActivePomLocations();
        this.activeSources = config.getActivePomLocations();
    }

    public Object setProperty( final String key, final Object value )
    {
        return properties.put( key, value );
    }

    public Object removeProperty( final String key )
    {
        return properties.remove( key );
    }

    public <T> T getProperty( final String key, final Class<T> type )
    {
        final Object value = properties.get( key );
        if ( value != null )
        {
            return type.cast( value );
        }

        return null;
    }

    public final String getId()
    {
        return id;
    }

    public final Set<URI> getActivePomLocations()
    {
        return activePomLocations;
    }

    public final Set<URI> getActiveSources()
    {
        return activeSources;
    }

    public final Iterable<URI> activePomLocations()
    {
        return activePomLocations;
    }

    public final Iterable<URI> activeSources()
    {
        return activeSources;
    }

    public final ProjectVersionRef selectVersion( final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        checkOpen();
        if ( !version.isConcrete() )
        {
            throw new IllegalArgumentException( "You cannot select a non-concrete version!" );
        }

        final SingleVersion old = selectedVersions.put( ref, version );
        if ( old == null || !old.equals( version ) )
        {
            fireSelectionAdded( ref, version );
        }

        final ProjectVersionRef updated = ref.selectVersion( version );
        return updated;
    }

    public final Map<ProjectVersionRef, SingleVersion> clearVersionSelections()
        throws GraphDriverException
    {
        checkOpen();
        final Map<ProjectVersionRef, SingleVersion> old =
            new HashMap<ProjectVersionRef, SingleVersion>( selectedVersions );

        selectedVersions.clear();
        fireSelectionsCleared();

        return old;
    }

    public final SingleVersion getSelectedVersion( final ProjectVersionRef ref )
    {
        checkOpen();
        return selectedVersions.get( ref.asProjectVersionRef() );
    }

    public final Map<ProjectVersionRef, SingleVersion> getVersionSelections()
    {
        return selectedVersions;
    }

    @Override
    public String toString()
    {
        return String.format( "EGraphSession (activePomLocations=[%s], activeSources=[%s])",
                              join( activePomLocations, ", " ), join( activeSources, ", " ) );
    }

    @Override
    public final int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result =
            prime * result + ( ( activePomLocations == null ) ? 0 : new HashSet<URI>( activePomLocations ).hashCode() );
        result = prime * result + ( ( activeSources == null ) ? 0 : new HashSet<URI>( activeSources ).hashCode() );
        return result;
    }

    @Override
    public final boolean equals( final Object obj )
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

    public final synchronized void close()
        throws GraphDriverException
    {
        if ( open )
        {
            clearVersionSelections();
            fireSessionClosed();
            open = false;
        }
    }

    private void fireSessionClosed()
        throws GraphDriverException
    {
        for ( final GraphSessionListener listener : listeners )
        {
            listener.sessionClosed( this );
        }
    }

    private void fireSelectionAdded( final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        for ( final GraphSessionListener listener : listeners )
        {
            listener.selectionAdded( this, ref, version );
        }
    }

    private void fireSelectionsCleared()
        throws GraphDriverException
    {
        for ( final GraphSessionListener listener : listeners )
        {
            listener.selectionsCleared( this );
        }
    }

    public final boolean isOpen()
    {
        return open;
    }

    protected final void checkOpen()
    {
        if ( !open )
        {
            throw new IllegalStateException( "Graph session instance is closed! You can only use open sessions." );
        }
    }

    public void addListener( final GraphSessionListener listener )
    {
        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }
    }

}
