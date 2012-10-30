package org.apache.maven.graph.effective.rel;

import java.io.Serializable;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class ParentRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    public ParentRelationship( final ProjectVersionRef declaring, final ProjectVersionRef target )
    {
        super( RelationshipType.PARENT, declaring, target, 0 );
    }

    @Override
    public String toString()
    {
        return String.format( "ParentRelationship [%s => %s]", getDeclaring(), getTarget() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new ArtifactRef( getTarget(), "pom", null, false );
    }

}
