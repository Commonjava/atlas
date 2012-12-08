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

    private List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>();

    public static final class Builder
    {
        private final List<ProjectRelationship<?>> cycle;

        public Builder( final ProjectRelationship<?>... rels )
        {
            cycle = new ArrayList<ProjectRelationship<?>>( Arrays.asList( rels ) );
        }

        public Builder( final List<ProjectRelationship<?>> rels )
        {
            cycle = new ArrayList<ProjectRelationship<?>>( rels );
        }

        public Builder( final Builder builder )
        {
            cycle = new ArrayList<ProjectRelationship<?>>( builder.cycle );
        }

        public Builder( final Builder builder, final int start )
        {
            cycle = new ArrayList<ProjectRelationship<?>>( builder.cycle );
            for ( int i = 0; i < start; i++ )
            {
                cycle.remove( 0 );
            }
        }

        public Builder with( final ProjectRelationship<?> rel )
        {
            cycle.add( rel );
            return this;
        }

        public Builder withoutLast()
        {
            cycle.remove( cycle.size() - 1 );
            return this;
        }

        public EProjectCycle build()
        {
            return new EProjectCycle( cycle );
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
        this.cycle = new ArrayList<ProjectRelationship<?>>( cycle );
    }

    public boolean contains( final ProjectRelationship<?> rel )
    {
        return cycle.contains( rel );
    }

    public boolean contains( final ProjectVersionRef ref )
    {
        for ( final ProjectRelationship<?> rel : cycle )
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
        return cycle.indexOf( rel );
    }

    public int indexOf( final ProjectVersionRef ref )
    {
        for ( int i = 0; i < cycle.size(); i++ )
        {
            final ProjectRelationship<?> rel = cycle.get( i );
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
        return cycle.iterator();
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        final Collection<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public Collection<ProjectRelationship<?>> getExactAllRelationships()
    {
        return new ArrayList<ProjectRelationship<?>>( cycle );
    }

    @Override
    public String toString()
    {
        return String.format( "Project cycle: [%s]", join( cycle, " -> " ) );
    }

    @Override
    public int hashCode()
    {
        final Set<ProjectRelationship<?>> cycle = new HashSet<ProjectRelationship<?>>( this.cycle );
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
        if ( cycle == null )
        {
            if ( other.cycle != null )
            {
                return false;
            }
        }
        else
        {

            final Set<ProjectRelationship<?>> cycle = new HashSet<ProjectRelationship<?>>( this.cycle );
            final Set<ProjectRelationship<?>> otherCycle = new HashSet<ProjectRelationship<?>>( other.cycle );

            if ( !cycle.equals( otherCycle ) )
            {
                return false;
            }
        }

        return true;
    }

}
