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
        int targetIdx = -1;
        for ( int i = 0; i < participants.size(); i++ )
        {
            final ProjectRelationship<?> rel = participants.get( i );
            if ( rel.getDeclaring()
                    .equals( ref ) )
            {
                return i;
            }

            // if we find the ref we're after as a TARGET, log it.
            // if, at the end, we haven't found it as a DECLARING ref, return
            // the index of the relationship that lists it as a target.
            if ( targetIdx < 0 && rel.getTarget()
                                     .asProjectVersionRef()
                                     .equals( ref ) )
            {
                targetIdx = i;
            }
        }

        return targetIdx;
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

            for ( final ProjectRelationship<?> rel : cycle )
            {
                if ( !otherCycle.contains( rel ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

}
