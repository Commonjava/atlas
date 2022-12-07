/**
 * Copyright (C) 2012-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.graph.rel;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;

/** <b>NOTE:</b> BOM relationships are actually marked as concrete.
 * This may be somewhat counter-intuitive, but they are structural (like a parent POM).
 * Therefore, managed isn't correct (despite Maven's unfortunate choice for location).
 */
public class SimpleBomRelationship
    extends AbstractSimpleProjectRelationship<BomRelationship, ProjectVersionRef>
        implements BomRelationship
{

    private static final long serialVersionUID = 1L;

    public SimpleBomRelationship( final Collection<URI> sources, final ProjectVersionRef d, final ProjectVersionRef t,
                                  final int index, final boolean inherited, final boolean mixin )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive,
        // but they're structural, so managed isn't quite correct (despite
        // Maven's unfortunate choice for location).
        super( sources, RelationshipType.BOM, d, t, index, false, inherited, mixin );
    }

    public SimpleBomRelationship( final URI source, final ProjectVersionRef d, final ProjectVersionRef t,
                                  final int index, final boolean inherited, final boolean mixin )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive,
        // but they're structural, so managed isn't quite correct (despite
        // Maven's unfortunate choice for location).
        super( source, RelationshipType.BOM, d, t, index, false, inherited, mixin );
    }

    public SimpleBomRelationship( final Collection<URI> sources, final URI pomLocation, final ProjectVersionRef d,
                                  final ProjectVersionRef t, final int index, final boolean inherited, final boolean mixin )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive,
        // but they're structural, so managed isn't quite correct (despite
        // Maven's unfortunate choice for location).
        super( sources, pomLocation, RelationshipType.BOM, d, t, index, false, inherited, mixin );
    }

    public SimpleBomRelationship( final URI source, final URI pomLocation, final ProjectVersionRef d,
                                  final ProjectVersionRef t, final int index, final boolean inherited, final boolean mixin )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive,
        // but they're structural, so managed isn't quite correct (despite
        // Maven's unfortunate choice for location).
        super( source, pomLocation, RelationshipType.BOM, d, t, index, false, inherited, mixin );
    }

    public SimpleBomRelationship( final BomRelationship relationship )
    {
        super( relationship );
    }

    @Override
    protected ProjectVersionRef cloneTarget( final ProjectVersionRef target )
    {
        return new SimpleProjectVersionRef( target );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget().asPomArtifact();
    }

    @Override
    public BomRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new SimpleBomRelationship( getSources(), ref, t, getIndex(), isInherited(), isMixin() );
    }

    @Override
    public BomRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();

        return new SimpleBomRelationship( getSources(), d, ref, getIndex(), isInherited(), isMixin() );
    }

    @Override
    public BomRelationship cloneFor( final ProjectVersionRef declaring )
    {
        return new SimpleBomRelationship( getSources(), getPomLocation(), declaring, getTarget(), getIndex(), isInherited(), isMixin() );
    }

    @Override
    public BomRelationship addSource( final URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimpleBomRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(),
                                          getIndex(), isInherited(), isMixin() );
    }

    @Override
    public BomRelationship addSources( final Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimpleBomRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(),
                                          getIndex(), isInherited(), isMixin() );
    }

    @Override
    public String toString()
    {
        return String.format( "BomRelationship [%s => %s]", getDeclaring(), getTarget() );
    }
}
