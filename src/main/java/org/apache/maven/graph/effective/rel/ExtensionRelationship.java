package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.VersionedProjectRef;

public final class ExtensionRelationship
    extends AbstractProjectRelationship<VersionedProjectRef>
{

    public ExtensionRelationship( final VersionedProjectRef declaring, final VersionedProjectRef target, final int index )
    {
        super( RelationshipType.EXTENSION, declaring, target, index );
    }

}
