package org.commonjava.maven.atlas.graph.util;

import java.net.URI;

import org.commonjava.maven.atlas.graph.rel.AbstractProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

final class SelfEdge
    extends AbstractProjectRelationship<ProjectVersionRef>
{

    private static final long serialVersionUID = 1L;

    SelfEdge( final ProjectVersionRef ref )
    {
        super( (URI) null, null, ref.asProjectVersionRef(), ref.asProjectVersionRef(), 0 );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget().asPomArtifact();
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        return new SelfEdge( getDeclaring().selectVersion( version, force ) );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
    {
        return selectTarget( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version, final boolean force )
    {
        return new SelfEdge( getDeclaring().selectVersion( version, force ) );
    }

}