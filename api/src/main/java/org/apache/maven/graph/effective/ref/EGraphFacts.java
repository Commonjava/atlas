/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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
package org.apache.maven.graph.effective.ref;

import static org.apache.commons.lang.StringUtils.join;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class EGraphFacts
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final Set<String> activeProfiles = new LinkedHashSet<String>();

    public EGraphFacts( final Collection<String> activeProfiles )
    {
        this.activeProfiles.addAll( activeProfiles );
    }

    public EGraphFacts( final String... activeProfiles )
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
        final EGraphFacts other = (EGraphFacts) obj;
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

    public String renderKeyPart()
    {
        return " " + join( activeProfiles, "," );
    }

}
