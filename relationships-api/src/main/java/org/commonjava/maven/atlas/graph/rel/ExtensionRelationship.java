/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.rel;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

public final class ExtensionRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    public ExtensionRelationship( final URI source, final ProjectVersionRef declaring, final ProjectVersionRef target,
                                  final int index )
    {
        super( source, RelationshipType.EXTENSION, declaring, target, index );
    }

    public ExtensionRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                                  final ProjectVersionRef target, final int index )
    {
        super( sources, RelationshipType.EXTENSION, declaring, target, index );
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

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring().selectVersion( version, force );
        final ProjectVersionRef t = getTarget();

        return new ExtensionRelationship( getSources(), d, t, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
    {
        return selectTarget( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring();
        final ProjectVersionRef t = getTarget().selectVersion( version, force );

        return new ExtensionRelationship( getSources(), d, t, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new ExtensionRelationship( getSources(), ref, t, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();

        return new ExtensionRelationship( getSources(), d, ref, getIndex() );
    }

}
