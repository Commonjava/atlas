package org.apache.maven.graph.effective.rel;

import java.io.Serializable;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class ExtensionRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    public ExtensionRelationship( final ProjectVersionRef declaring, final ProjectVersionRef target, final int index )
    {
        super( RelationshipType.EXTENSION, declaring, target, index );
    }

    @Override
    public String toString()
    {
        return String.format( "ExtensionRelationship [%s => %s (index=%s)]", getDeclaring(), getTarget(), getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new ArtifactRef( getTarget(), null, null, false );
    }

}
