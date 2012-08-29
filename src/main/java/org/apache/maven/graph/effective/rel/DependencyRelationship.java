package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class DependencyRelationship
    extends AbstractProjectRelationship<ArtifactRef>
{

    private final boolean managed;

    private final DependencyScope scope;

    public DependencyRelationship( final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed )
    {
        super( RelationshipType.DEPENDENCY, declaring, target, index );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.managed = managed;
    }

    public final boolean isManaged()
    {
        return managed;
    }

    public final DependencyScope getScope()
    {
        return scope;
    }

    @Override
    public synchronized ProjectRelationship<ArtifactRef> cloneFor( final ProjectVersionRef projectRef )
    {
        return new DependencyRelationship( projectRef, getTarget(), scope, getIndex(), managed );
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
        final DependencyRelationship other = (DependencyRelationship) obj;
        if ( managed != other.managed )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "DependencyRelationship [%s => %s (managed=%s, scope=%s, index=%s)]", getDeclaring(),
                              getTarget(), managed, scope, getIndex() );
    }

}
