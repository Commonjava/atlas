package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.VersionedProjectRef;

public final class ParentRelationship
    extends AbstractProjectRelationship<VersionedProjectRef>
{

    public ParentRelationship( final VersionedProjectRef declaring, final VersionedProjectRef target )
    {
        super( RelationshipType.PARENT, declaring, target, 0 );
    }

}
