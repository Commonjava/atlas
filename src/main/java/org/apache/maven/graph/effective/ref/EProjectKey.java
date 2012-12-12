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

import java.io.Serializable;

import org.apache.maven.graph.common.ref.ProjectVersionRef;

public class EProjectKey
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final ProjectVersionRef project;

    private final EGraphFacts facts;

    public EProjectKey( final ProjectVersionRef project, final EGraphFacts facts )
    {
        this.project = project;
        this.facts = facts;
    }

    public EProjectKey( final ProjectVersionRef project )
    {
        this( project, new EGraphFacts() );
    }

    public final ProjectVersionRef getProject()
    {
        return project;
    }

    public final EGraphFacts getFacts()
    {
        return facts;
    }

    public String renderKey()
    {
        return project.toString() + facts.renderKeyPart();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( facts == null ) ? 0 : facts.hashCode() );
        result = prime * result + ( ( project == null ) ? 0 : project.hashCode() );
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
        final EProjectKey other = (EProjectKey) obj;
        if ( facts == null )
        {
            if ( other.facts != null )
            {
                return false;
            }
        }
        else if ( !facts.equals( other.facts ) )
        {
            return false;
        }
        if ( project == null )
        {
            if ( other.project != null )
            {
                return false;
            }
        }
        else if ( !project.equals( other.project ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "EProjectKey [project=%s, facts=%s]", project, facts );
    }

}
