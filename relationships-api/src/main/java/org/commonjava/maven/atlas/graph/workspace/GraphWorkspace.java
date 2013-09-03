package org.commonjava.maven.atlas.graph.workspace;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class GraphWorkspace
    implements Closeable
{

    private final GraphWorkspaceConfiguration config;

    private final transient Map<String, Object> properties = new HashMap<String, Object>();

    private String id;

    private boolean open = true;

    private final transient List<GraphWorkspaceListener> listeners = new ArrayList<GraphWorkspaceListener>();

    private long lastAccess = System.currentTimeMillis();

    private final GraphDatabaseDriver dbDriver;

    public GraphWorkspace( final String id, final GraphWorkspaceConfiguration config, final GraphDatabaseDriver dbDriver )
    {
        this.id = id;
        this.config = config;
        this.dbDriver = dbDriver;
    }

    public GraphWorkspace( final String id, final GraphWorkspaceConfiguration config, final GraphDatabaseDriver dbDriver, final long lastAccess )
    {
        this.id = id;
        this.config = config;
        this.dbDriver = dbDriver;
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

    public final ProjectVersionRef selectVersion( final ProjectVersionRef ref, final ProjectVersionRef selected )
        throws GraphDriverException
    {
        checkOpen();
        if ( !selected.isSpecificVersion() )
        {
            throw new IllegalArgumentException( "You cannot select a non-concrete version!" );
        }

        dbDriver.selectVersionFor( ref, selected );
        fireSelectionAdded( ref, selected );

        fireAccessed();
        return selected;
    }

    public final boolean selectVersionForAll( final ProjectRef ref, final ProjectVersionRef selected )
        throws GraphDriverException
    {
        checkOpen();
        if ( !selected.isSpecificVersion() )
        {
            throw new IllegalArgumentException( "You cannot select a non-concrete version!" );
        }

        dbDriver.selectVersionForAll( ref, selected );
        fireWildcardSelectionAdded( ref, selected );

        fireAccessed();
        return true;
    }

    public final void clearVersionSelections()
    {
        checkOpen();
        dbDriver.clearSelectedVersions();
        fireAccessed();
        fireSelectionsCleared();
    }

    public final ProjectVersionRef getSelection( final ProjectVersionRef ref )
    {
        checkOpen();
        fireAccessed();
        return dbDriver.getSelectedFor( ref );
    }

    public final Map<ProjectVersionRef, ProjectVersionRef> getVersionSelections()
    {
        fireAccessed();
        return dbDriver.getSelections();
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

    @Override
    public final synchronized void close()
        throws IOException
    {
        if ( open )
        {
            getDatabase().close();
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

    private void fireWildcardSelectionAdded( final ProjectRef ref, final ProjectVersionRef selected )
        throws GraphDriverException
    {
        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.wildcardSelectionAdded( this, ref, selected );
        }
    }

    private void fireSelectionAdded( final ProjectVersionRef ref, final ProjectVersionRef selected )
        throws GraphDriverException
    {
        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.selectionAdded( this, ref, selected );
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
        return dbDriver.hasSelectionFor( ref );
    }

    public boolean hasSelectionForAll( final ProjectRef ref )
    {
        return dbDriver.hasSelectionForAll( ref );
    }

    public boolean isForceVersions()
    {
        return config.isForceVersions();
    }

    public GraphDatabaseDriver getDatabase()
    {
        return dbDriver;
    }

    public GraphWorkspaceConfiguration getConfiguration()
    {
        return config;
    }

}
