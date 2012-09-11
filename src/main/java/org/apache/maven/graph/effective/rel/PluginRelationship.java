package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class PluginRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
{

    private final boolean managed;

    private final boolean reporting;

    public PluginRelationship( final ProjectVersionRef declaring, final ProjectVersionRef target, final int index,
                               final boolean managed )
    {
        this( declaring, target, index, managed, false );
    }

    public PluginRelationship( final ProjectVersionRef declaring, final ProjectVersionRef target, final int index,
                               final boolean managed, final boolean reporting )
    {
        super( RelationshipType.PLUGIN, declaring, target, index );
        this.managed = managed;
        this.reporting = reporting;
    }

    public final boolean isReporting()
    {
        return reporting;
    }

    public final boolean isManaged()
    {
        return managed;
    }

    @Override
    public synchronized ProjectRelationship<ProjectVersionRef> cloneFor( final ProjectVersionRef projectRef )
    {
        return new PluginRelationship( projectRef, getTarget(), getIndex(), managed, reporting );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( managed ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final PluginRelationship other = (PluginRelationship) obj;
        if ( managed != other.managed )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "PluginRelationship [%s => %s (managed=%s, index=%s)]", getDeclaring(), getTarget(),
                              managed, getIndex() );
    }

}
