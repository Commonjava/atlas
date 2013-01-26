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
package org.apache.maven.graph.effective;

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.maven.graph.effective.util.EGraphUtils.filterTerminalParents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class EProjectCycle
    implements Iterable<ProjectRelationship<?>>, EProjectRelationshipCollection
{

    private static final long serialVersionUID = 1L;

    private List<ProjectRelationship<?>> participants = new ArrayList<ProjectRelationship<?>>();

    public static final class Builder
    {
        private final List<ProjectRelationship<?>> participants;

        public Builder( final ProjectRelationship<?>... rels )
        {
            participants = new ArrayList<ProjectRelationship<?>>( Arrays.asList( rels ) );
        }

        public Builder( final List<ProjectRelationship<?>> rels )
        {
            participants = new ArrayList<ProjectRelationship<?>>( rels );
        }

        public Builder( final Builder builder )
        {
            participants = new ArrayList<ProjectRelationship<?>>( builder.participants );
        }

        public Builder( final Builder builder, final int start )
        {
            participants = new ArrayList<ProjectRelationship<?>>( builder.participants );
            for ( int i = 0; i < start; i++ )
            {
                participants.remove( 0 );
            }
        }

        public Builder with( final ProjectRelationship<?> rel )
        {
            participants.add( rel );
            return this;
        }

        public Builder withoutLast()
        {
            participants.remove( participants.size() - 1 );
            return this;
        }

        public EProjectCycle build()
        {
            return new EProjectCycle( participants );
        }

        public int indexOf( final ProjectVersionRef ref )
        {
            return build().indexOf( ref );
        }

        public int indexOf( final ProjectRelationship<?> rel )
        {
            return build().indexOf( rel );
        }

        public boolean contains( final ProjectVersionRef ref )
        {
            return build().contains( ref );
        }

        public boolean contains( final ProjectRelationship<?> rel )
        {
            return build().contains( rel );
        }
    }

    public EProjectCycle( final List<ProjectRelationship<?>> cycle )
    {
        this.participants = new ArrayList<ProjectRelationship<?>>( cycle );
    }

    public boolean contains( final ProjectRelationship<?> rel )
    {
        return participants.contains( rel );
    }

    public boolean contains( final ProjectVersionRef ref )
    {
        for ( final ProjectRelationship<?> rel : participants )
        {
            if ( rel.getDeclaring()
                    .equals( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    public int indexOf( final ProjectRelationship<?> rel )
    {
        return participants.indexOf( rel );
    }

    public int indexOf( final ProjectVersionRef ref )
    {
        for ( int i = 0; i < participants.size(); i++ )
        {
            final ProjectRelationship<?> rel = participants.get( i );
            if ( rel.getDeclaring()
                    .equals( ref ) )
            {
                return i;
            }
        }

        return -1;
    }

    public Iterator<ProjectRelationship<?>> iterator()
    {
        return participants.iterator();
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        final Collection<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public Collection<ProjectRelationship<?>> getExactAllRelationships()
    {
        return new ArrayList<ProjectRelationship<?>>( participants );
    }

    public Set<ProjectVersionRef> getAllParticipatingProjects()
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : participants )
        {
            refs.add( rel.getDeclaring() );
            refs.add( rel.getTarget()
                         .asProjectVersionRef() );
        }

        return refs;
    }

    @Override
    public String toString()
    {
        return String.format( "Project cycle: [%s]", join( participants, " -> " ) );
    }

    @Override
    public int hashCode()
    {
        final Set<ProjectRelationship<?>> cycle = new HashSet<ProjectRelationship<?>>( this.participants );
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( cycle == null ) ? 0 : cycle.hashCode() );
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
        final EProjectCycle other = (EProjectCycle) obj;
        if ( participants == null )
        {
            if ( other.participants != null )
            {
                return false;
            }
        }
        else
        {

            final Set<ProjectRelationship<?>> cycle = new HashSet<ProjectRelationship<?>>( this.participants );
            final Set<ProjectRelationship<?>> otherCycle = new HashSet<ProjectRelationship<?>>( other.participants );

            if ( !cycle.equals( otherCycle ) )
            {
                return false;
            }
        }

        return true;
    }

}
