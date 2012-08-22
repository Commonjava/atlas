package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.VersionedProjectRef;

public final class PluginRelationship
    extends AbstractProjectRelationship<VersionedProjectRef>
{

    private final boolean managed;

    public PluginRelationship( final VersionedProjectRef declaring, final VersionedProjectRef target, final int index,
                               final boolean managed )
    {
        super( RelationshipType.PLUGIN, declaring, target, index );
        this.managed = managed;
    }

    public final boolean isManaged()
    {
        return managed;
    }

    @Override
    public synchronized ProjectRelationship<VersionedProjectRef> cloneFor( final VersionedProjectRef projectRef )
    {
        return new PluginRelationship( projectRef, getTarget(), getIndex(), managed );
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

}
