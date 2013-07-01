package org.apache.maven.graph.effective.workspace;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.effective.util.RelationshipUtils;

public final class GraphWorkspaceConfiguration
{
    public static final Set<URI> DEFAULT_POM_LOCATIONS = Collections.singleton( RelationshipUtils.POM_ROOT_URI );

    public static final Set<URI> DEFAULT_SOURCES = Collections.singleton( RelationshipUtils.UNKNOWN_SOURCE_URI );

    private Set<URI> activePomLocations;

    private Set<URI> activeSources;

    private boolean forceVersions = true;

    public GraphWorkspaceConfiguration withPomLocations( final URI... pomLocations )
    {
        initActivePomLocations();
        this.activePomLocations.addAll( Arrays.asList( pomLocations ) );
        return this;
    }

    private void initActivePomLocations()
    {
        if ( activePomLocations == null )
        {
            activePomLocations = new HashSet<URI>();
        }
    }

    private void initActiveSources()
    {
        if ( activeSources == null )
        {
            activeSources = new HashSet<URI>();
        }
    }

    public GraphWorkspaceConfiguration withForcedVersions( final boolean forced )
    {
        this.forceVersions = forced;
        return this;
    }

    public boolean isForceVersions()
    {
        return forceVersions;
    }

    public GraphWorkspaceConfiguration withPomLocations( final Collection<URI> pomLocations )
    {
        initActivePomLocations();
        this.activePomLocations.addAll( pomLocations );
        return this;
    }

    public GraphWorkspaceConfiguration withPomLocation( final URI pomLocation )
    {
        initActivePomLocations();
        this.activePomLocations.add( pomLocation );
        return this;
    }

    public GraphWorkspaceConfiguration withSources( final URI... sources )
    {
        initActiveSources();
        this.activeSources.addAll( Arrays.asList( sources ) );
        return this;
    }

    public GraphWorkspaceConfiguration withSources( final Collection<URI> sources )
    {
        initActiveSources();
        this.activeSources.addAll( sources );
        return this;
    }

    public GraphWorkspaceConfiguration withSource( final URI source )
    {
        initActiveSources();
        this.activeSources.add( source );
        return this;
    }

    public Set<URI> getActivePomLocations()
    {
        return activePomLocations == null ? DEFAULT_POM_LOCATIONS : activePomLocations;
    }

    public Set<URI> getActiveSources()
    {
        return activeSources == null ? DEFAULT_SOURCES : activeSources;
    }

    @Override
    public int hashCode()
    {
        int result = 13;

        if ( activePomLocations != null )
        {
            result += activePomLocations.hashCode();
        }

        result -= activeSources.hashCode();

        return result;
    }

    @Override
    public boolean equals( final Object other )
    {
        if ( this == other )
        {
            return true;
        }
        if ( !( other instanceof GraphWorkspaceConfiguration ) )
        {
            return false;
        }
        final GraphWorkspaceConfiguration o = (GraphWorkspaceConfiguration) other;
        if ( !compareSets( activePomLocations, o.activePomLocations ) )
        {
            return false;
        }
        if ( !compareSets( activeSources, o.activeSources ) )
        {
            return false;
        }

        return true;
    }

    private boolean compareSets( final Set<URI> first, final Set<URI> second )
    {
        if ( ( first == null && second != null ) || ( first != null && second == null ) )
        {
            return false;
        }

        if ( first != null && second != null && first.size() != second.size() )
        {
            return false;
        }

        for ( final URI f : first )
        {
            if ( !second.contains( f ) )
            {
                return false;
            }
        }

        return true;
    }

    public int getActivePomLocationCount()
    {
        return activePomLocations == null ? 0 : activePomLocations.size();
    }

    public int getActiveSourceCount()
    {
        return activeSources == null ? 0 : activeSources.size();
    }

}