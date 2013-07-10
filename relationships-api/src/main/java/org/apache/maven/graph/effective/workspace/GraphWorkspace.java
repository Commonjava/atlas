package org.apache.maven.graph.effective.workspace;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.spi.GraphDriverException;

public final class GraphWorkspace
{

    private final GraphWorkspaceConfiguration config;

    private Map<ProjectVersionRef, SingleVersion> selectedVersions = new HashMap<ProjectVersionRef, SingleVersion>();

    private Map<ProjectRef, SingleVersion> wildcardSelectedVersions = new HashMap<ProjectRef, SingleVersion>();

    private final transient Map<String, Object> properties = new HashMap<String, Object>();

    private String id;

    private boolean open = true;

    private final transient List<GraphWorkspaceListener> listeners = new ArrayList<GraphWorkspaceListener>();

    private long lastAccess = System.currentTimeMillis();

    public GraphWorkspace( final String id, final GraphWorkspaceConfiguration config )
    {
        this.id = id;
        this.config = config;
    }

    public GraphWorkspace( final String id, final GraphWorkspaceConfiguration config, final long lastAccess )
    {
        this.id = id;
        this.config = config;
        this.lastAccess = lastAccess;
    }

    protected void setLastAccess( final long lastAccess )
    {
        this.lastAccess = lastAccess;
    }

    protected void setId( final String id )
    {
        this.id = id;
    }

    protected void setSelectedVersions( final Map<ProjectVersionRef, SingleVersion> selections )
    {
        this.selectedVersions = selections;
    }

    protected void setWildcardSelectedVersions( final Map<ProjectRef, SingleVersion> selections )
    {
        this.wildcardSelectedVersions = selections;
    }

    public void touch()
    {
        fireAccessed();
    }

    public GraphWorkspace addActivePomLocation( final URI location )
    {
        final int before = config.getActivePomLocationCount();

        config.withPomLocations( location );

        if ( config.getActivePomLocationCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActivePomLocations( final Collection<URI> locations )
    {
        final int before = config.getActivePomLocationCount();

        config.withPomLocations( locations );

        if ( config.getActivePomLocationCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActivePomLocations( final URI... locations )
    {
        final int before = config.getActivePomLocationCount();

        config.withPomLocations( locations );

        if ( config.getActivePomLocationCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActiveSources( final Collection<URI> sources )
    {
        final int before = config.getActiveSourceCount();

        config.withSources( sources );

        if ( config.getActiveSourceCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActiveSources( final URI... sources )
    {
        final int before = config.getActiveSourceCount();

        config.withSources( sources );

        if ( config.getActiveSourceCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActiveSource( final URI source )
    {
        final int before = config.getActiveSourceCount();

        config.withSources( source );

        if ( config.getActiveSourceCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public long getLastAccess()
    {
        return lastAccess;
    }

    /** NOTE: Non-durable!
     */
    public Object setProperty( final String key, final Object value )
    {
        fireAccessed();
        return properties.put( key, value );
    }

    public Object removeProperty( final String key )
    {
        fireAccessed();
        return properties.remove( key );
    }

    public <T> T getProperty( final String key, final Class<T> type )
    {
        fireAccessed();
        final Object value = properties.get( key );
        if ( value != null )
        {
            return type.cast( value );
        }

        return null;
    }

    public <T> T getProperty( final String key, final Class<T> type, final T def )
    {
        fireAccessed();
        final Object value = properties.get( key );
        if ( value != null )
        {
            return type.cast( value );
        }

        return def;
    }

    public final String getId()
    {
        return id;
    }

    public final Set<URI> getActivePomLocations()
    {
        fireAccessed();
        return config.getActivePomLocations();
    }

    public final Set<URI> getActiveSources()
    {
        fireAccessed();
        return config.getActiveSources();
    }

    public final Iterable<URI> activePomLocations()
    {
        fireAccessed();
        return config.getActivePomLocations();
    }

    public final Iterable<URI> activeSources()
    {
        fireAccessed();
        return config.getActiveSources();
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

        fireAccessed();
        final ProjectVersionRef updated = ref.selectVersion( version, config.isForceVersions() );
        return updated;
    }

    public final void selectVersionForAll( final ProjectRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        checkOpen();
        if ( !version.isConcrete() )
        {
            throw new IllegalArgumentException( "You cannot select a non-concrete version!" );
        }

        final SingleVersion old = wildcardSelectedVersions.put( ref, version );
        if ( old == null || !old.equals( version ) )
        {
            fireWildcardSelectionAdded( ref, version );
        }

        fireAccessed();
    }

    public final Map<ProjectVersionRef, SingleVersion> clearVersionSelections()
    {
        checkOpen();
        final Map<ProjectVersionRef, SingleVersion> old =
            new HashMap<ProjectVersionRef, SingleVersion>( selectedVersions );

        selectedVersions.clear();
        fireAccessed();
        fireSelectionsCleared();

        return old;
    }

    public final SingleVersion getSelectedVersion( final ProjectVersionRef ref )
    {
        checkOpen();
        fireAccessed();
        SingleVersion version = selectedVersions.get( ref.asProjectVersionRef() );

        if ( version == null )
        {
            version = wildcardSelectedVersions.get( ref.asProjectRef() );
        }

        return version;
    }

    public final Map<ProjectVersionRef, SingleVersion> getVersionSelections()
    {
        fireAccessed();
        return selectedVersions;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphWorkspace (id=%s, config=[%s])", id, config );
    }

    @Override
    public final int hashCode()
    {
        return 31 * id.hashCode();
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

        final GraphWorkspace other = (GraphWorkspace) obj;
        if ( !id.equals( other.id ) )
        {
            return false;
        }

        return true;
    }

    public final synchronized void close()
    {
        if ( open )
        {
            clearVersionSelections();
            open = false;
            fireClosed();
        }
    }

    private void fireClosed()
    {
        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.closed( this );
        }
    }

    private void fireAccessed()
    {
        lastAccess = System.currentTimeMillis();
        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.accessed( this );
        }
    }

    private void fireWildcardSelectionAdded( final ProjectRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.wildcardSelectionAdded( this, ref, version );
        }
    }

    private void fireSelectionAdded( final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.selectionAdded( this, ref, version );
        }
    }

    private void fireSelectionsCleared()
    {
        for ( final GraphWorkspaceListener listener : listeners )
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

    public GraphWorkspace addListener( final GraphWorkspaceListener listener )
    {
        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }

        return this;
    }

    public boolean hasSelection( final ProjectVersionRef ref )
    {
        return selectedVersions.containsKey( ref );
    }

    public boolean hasSelectionForAll( final ProjectRef ref )
    {
        return wildcardSelectedVersions.containsKey( ref );
    }

    public boolean isForceVersions()
    {
        return config.isForceVersions();
    }

}
