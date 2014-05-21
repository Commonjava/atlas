/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.workspace;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.util.RelationshipUtils;

public final class GraphWorkspaceConfiguration
{
    private Set<URI> activePomLocations;

    private Set<URI> activeSources;

    private long lastAccess = System.currentTimeMillis();

    private Map<String, String> properties;

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
            activePomLocations = new HashSet<URI>( GraphWorkspace.DEFAULT_POM_LOCATIONS );
        }
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

    public GraphWorkspaceConfiguration withoutRootPomLocation()
    {
        initActivePomLocations();
        this.activePomLocations.remove( RelationshipUtils.POM_ROOT_URI );
        return this;
    }

    public GraphWorkspaceConfiguration withSources( final URI... sources )
    {
        initActiveSources();
        this.activeSources.addAll( Arrays.asList( sources ) );
        return this;
    }

    private void initActiveSources()
    {
        if ( activeSources == null )
        {
            activeSources = new HashSet<URI>( GraphWorkspace.DEFAULT_SOURCES );
        }
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
        return activePomLocations == null ? GraphWorkspace.DEFAULT_POM_LOCATIONS : activePomLocations;
    }

    public Set<URI> getActiveSources()
    {
        return activeSources == null ? GraphWorkspace.DEFAULT_SOURCES : activeSources;
    }

    @Override
    public int hashCode()
    {
        int result = 13;

        if ( activePomLocations != null )
        {
            result += activePomLocations.hashCode();
        }

        if ( activeSources != null )
        {
            result -= activeSources.hashCode();
        }

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

    @Override
    public String toString()
    {
        return String.format( "GraphWorkspaceConfiguration [activePomLocations=%s, activeSources=%s]",
                              activePomLocations, activeSources );
    }

    public void setLastAccess( final long lastAccess )
    {
        this.lastAccess = lastAccess;
    }

    public long getLastAccess()
    {
        return lastAccess;
    }

    public synchronized String setProperty( final String key, final String value )
    {
        if ( properties == null )
        {
            properties = new HashMap<String, String>();
        }

        return properties.put( key, value );
    }

    public String getProperty( final String key )
    {
        return properties.get( key );
    }

    public String getProperty( final String key, final String def )
    {
        final String value = properties.get( key );
        return value != null ? value : def;
    }

    public synchronized String removeProperty( final String key )
    {
        if ( properties == null )
        {
            return null;
        }

        return properties.remove( key );
    }

    public void withoutPomLocations( final URI... locations )
    {
        if ( activePomLocations != null )
        {
            for ( final URI location : locations )
            {
                activePomLocations.remove( location );
            }
        }
    }

    public void withoutPomLocations( final Collection<URI> locations )
    {
        if ( activePomLocations != null )
        {
            for ( final URI location : locations )
            {
                activePomLocations.remove( location );
            }
        }
    }

    public void withoutSources( final Collection<URI> sources )
    {
        if ( activeSources != null )
        {
            for ( final URI source : sources )
            {
                activeSources.remove( source );
            }
        }
    }

    public void withoutSources( final URI... sources )
    {
        if ( activeSources != null )
        {
            for ( final URI source : sources )
            {
                activeSources.remove( source );
            }
        }
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

}
