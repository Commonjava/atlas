/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.workspace;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;

public final class GraphWorkspace
    implements Closeable, Serializable
{

    private static final long serialVersionUID = 1L;

    public static final Set<URI> DEFAULT_POM_LOCATIONS = Collections.singleton( RelationshipUtils.POM_ROOT_URI );

    public static final Set<URI> DEFAULT_SOURCES = Collections.singleton( RelationshipUtils.UNKNOWN_SOURCE_URI );

    //    private final GraphWorkspaceConfiguration config;

    private String id;

    private transient GraphDatabaseDriver dbDriver;

    private transient boolean open = true;

    private transient List<GraphWorkspaceListener> listeners;

    public GraphWorkspace( final String id, final GraphDatabaseDriver dbDriver )
    {
        this.id = id;
        //        this.config = config;
        this.dbDriver = dbDriver;
    }

    public GraphWorkspace( final String id, final GraphDatabaseDriver dbDriver, final long lastAccess )
    {
        this.id = id;
        //        this.config = config;
        this.dbDriver = dbDriver;
        this.dbDriver.setLastAccess( lastAccess );
    }

    private void initActivePomLocations()
    {
        if ( dbDriver.getActivePomLocationCount() < 1 )
        {
            dbDriver.addActivePomLocations( DEFAULT_POM_LOCATIONS );
        }
    }

    protected void setLastAccess( final long lastAccess )
    {
        this.dbDriver.setLastAccess( lastAccess );
    }

    protected void setId( final String id )
    {
        this.id = id;
    }

    public void detach()
    {
        fireDetached();
    }

    public void touch()
    {
        fireAccessed();
    }

    public GraphWorkspace addActivePomLocation( final URI location )
    {
        final int before = dbDriver.getActivePomLocationCount();

        initActivePomLocations();
        dbDriver.addActivePomLocations( location );

        if ( dbDriver.getActivePomLocationCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActivePomLocations( final Collection<URI> locations )
    {
        final int before = dbDriver.getActivePomLocationCount();

        initActivePomLocations();
        dbDriver.addActivePomLocations( locations );

        if ( dbDriver.getActivePomLocationCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActivePomLocations( final URI... locations )
    {
        final int before = dbDriver.getActivePomLocationCount();

        initActivePomLocations();
        dbDriver.addActivePomLocations( locations );

        if ( dbDriver.getActivePomLocationCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace removeRootPomLocation()
    {
        dbDriver.removeActivePomLocations( RelationshipUtils.POM_ROOT_URI );
        return this;
    }

    public GraphWorkspace addActiveSources( final Collection<URI> sources )
    {
        final int before = dbDriver.getActiveSourceCount();

        dbDriver.addActiveSources( sources );

        if ( dbDriver.getActiveSourceCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActiveSources( final URI... sources )
    {
        final int before = dbDriver.getActiveSourceCount();

        dbDriver.addActiveSources( sources );

        if ( dbDriver.getActiveSourceCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public GraphWorkspace addActiveSource( final URI source )
    {
        final int before = dbDriver.getActiveSourceCount();

        dbDriver.addActiveSources( source );

        if ( dbDriver.getActiveSourceCount() != before )
        {
            fireAccessed();
        }

        return this;
    }

    public long getLastAccess()
    {
        return dbDriver.getLastAccess();
    }

    public String setProperty( final String key, final String value )
    {
        fireAccessed();
        return dbDriver.setProperty( key, value );
    }

    public String removeProperty( final String key )
    {
        fireAccessed();
        return dbDriver.removeProperty( key );
    }

    public String getProperty( final String key )
    {
        fireAccessed();
        return dbDriver.getProperty( key );
    }

    public String getProperty( final String key, final String def )
    {
        fireAccessed();
        return dbDriver.getProperty( key, def );
    }

    public final String getId()
    {
        return id;
    }

    public final Set<URI> getActivePomLocations()
    {
        fireAccessed();
        final Set<URI> result = dbDriver.getActivePomLocations();
        return result == null ? DEFAULT_POM_LOCATIONS : result;
    }

    public final Set<URI> getActiveSources()
    {
        fireAccessed();
        final Set<URI> result = dbDriver.getActiveSources();
        return result == null ? DEFAULT_SOURCES : result;
    }

    public final Iterable<URI> activePomLocations()
    {
        fireAccessed();
        return dbDriver.getActivePomLocations();
    }

    public final Iterable<URI> activeSources()
    {
        fireAccessed();
        return dbDriver.getActiveSources();
    }

    @Override
    public String toString()
    {
        return String.format( "GraphWorkspace (id=%s, driver=[%s])", id, dbDriver );
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
            fireClosed();
            open = false;
        }
    }

    private void fireClosed()
    {
        if ( listeners == null )
        {
            return;
        }

        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.closed( this );
        }
    }

    private void fireAccessed()
    {
        dbDriver.setLastAccess( System.currentTimeMillis() );

        if ( listeners == null )
        {
            return;
        }

        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.accessed( this );
        }
    }

    private void fireDetached()
    {
        dbDriver.setLastAccess( System.currentTimeMillis() );

        if ( listeners == null )
        {
            return;
        }

        for ( final GraphWorkspaceListener listener : listeners )
        {
            listener.detached( this );
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
        if ( listeners == null )
        {
            listeners = new ArrayList<GraphWorkspaceListener>();
        }

        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }

        return this;
    }

    public GraphDatabaseDriver getDatabase()
    {
        return dbDriver;
    }

    public void registerView( final GraphView view )
    {
        dbDriver.registerView( view );
    }

    //    public GraphWorkspaceConfiguration getConfiguration()
    //    {
    //        return config;
    //    }

    public void reattach( final GraphDatabaseDriver driver )
    {
        this.dbDriver = driver;
    }

}
