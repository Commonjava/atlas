package org.apache.maven.graph.effective.session;

import static org.apache.commons.lang.StringUtils.join;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public abstract class EGraphSession
{

    private final Set<URI> activePomLocations;

    private final Set<URI> activeSources;

    private final Map<ProjectVersionRef, SingleVersion> selectedVersions =
        new HashMap<ProjectVersionRef, SingleVersion>();

    private final EGraphDriver driver;

    private final String id;

    private boolean open = true;

    protected EGraphSession( final String id, final EGraphDriver driver, final EGraphSessionConfiguration config )
    {
        this.id = id;
        this.driver = driver;
        this.activePomLocations = config.getActivePomLocations();
        this.activeSources = config.getActivePomLocations();
    }

    public final EGraphDriver getDriver()
    {
        return driver;
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
            selectionAdded( ref, version );
        }

        final ProjectVersionRef updated = ref.selectVersion( version );
        return updated;
    }

    public final Map<ProjectVersionRef, SingleVersion> clearVersionSelections()
    {
        checkOpen();
        final Map<ProjectVersionRef, SingleVersion> old =
            new HashMap<ProjectVersionRef, SingleVersion>( selectedVersions );

        selectedVersions.clear();
        selectionsCleared();

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
        result = prime + result + ( ( driver == null ) ? 0 : driver.hashCode() );
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
        if ( driver == null )
        {
            if ( other.driver != null )
            {
                return false;
            }
        }
        else if ( !driver.equals( other.driver ) )
        {
            return false;
        }

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
            sessionClosed();
            open = false;
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

    protected void selectionAdded( final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
    }

    protected void sessionClosed()
    {
    }

    protected void selectionsCleared()
    {
    }

}
