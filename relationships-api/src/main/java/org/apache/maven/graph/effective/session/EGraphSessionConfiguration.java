package org.apache.maven.graph.effective.session;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class EGraphSessionConfiguration
{
    private final Set<URI> activePomLocations = new LinkedHashSet<URI>();

    private final Set<URI> activeSources = new LinkedHashSet<URI>();

    public EGraphSessionConfiguration withPomLocations( final URI... pomLocations )
    {
        this.activePomLocations.addAll( Arrays.asList( pomLocations ) );
        return this;
    }

    public EGraphSessionConfiguration withPomLocations( final Collection<URI> pomLocations )
    {
        this.activePomLocations.addAll( pomLocations );
        return this;
    }

    public EGraphSessionConfiguration withPomLocation( final URI pomLocation )
    {
        this.activePomLocations.add( pomLocation );
        return this;
    }

    public EGraphSessionConfiguration withSources( final URI... sources )
    {
        this.activeSources.addAll( Arrays.asList( sources ) );
        return this;
    }

    public EGraphSessionConfiguration withSources( final Collection<URI> sources )
    {
        this.activeSources.addAll( sources );
        return this;
    }

    public EGraphSessionConfiguration withSource( final URI source )
    {
        this.activeSources.add( source );
        return this;
    }

    public Set<URI> getActivePomLocations()
    {
        return activePomLocations;
    }

    public Set<URI> getActiveSources()
    {
        return activeSources;
    }

}