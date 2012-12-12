/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
