package org.apache.maven.graph.effective.ref;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EProjectFacts
{

    private final Set<String> activeProfiles = new HashSet<String>();

    public EProjectFacts( final Collection<String> activeProfiles )
    {
        this.activeProfiles.addAll( activeProfiles );
    }

    public EProjectFacts( final String... activeProfiles )
    {
        this.activeProfiles.addAll( Arrays.asList( activeProfiles ) );
    }

    public Set<String> getActiveProfiles()
    {
        return activeProfiles;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( activeProfiles == null ) ? 0 : activeProfiles.hashCode() );
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
        final EProjectFacts other = (EProjectFacts) obj;
        if ( activeProfiles == null )
        {
            if ( other.activeProfiles != null )
            {
                return false;
            }
        }
        else if ( !activeProfiles.equals( other.activeProfiles ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "(active profiles=%s)", activeProfiles );
    }

}
