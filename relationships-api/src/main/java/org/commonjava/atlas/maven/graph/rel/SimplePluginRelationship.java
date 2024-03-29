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

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;

public final class SimplePluginRelationship
    extends AbstractSimpleProjectRelationship<PluginRelationship, ProjectVersionRef>
    implements Serializable, PluginRelationship
{

    private static final long serialVersionUID = 1L;

    private final boolean reporting;

    public SimplePluginRelationship( final URI source, final ProjectVersionRef declaring,
                                     final ProjectVersionRef target, final int index, final boolean managed,
                                     final boolean inherited )
    {
        this( source, declaring, target, index, managed, false, inherited );
    }

    public SimplePluginRelationship( final URI source, final ProjectVersionRef declaring,
                                     final ProjectVersionRef target, final int index, final boolean managed,
                                     final boolean reporting, final boolean inherited )
    {
        super( source, RelationshipType.PLUGIN, declaring, target, index, managed, inherited, false );
        this.reporting = reporting;
    }

    public SimplePluginRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                                     final ProjectVersionRef target, final int index, final boolean managed,
                                     final boolean inherited )
    {
        this( source, pomLocation, declaring, target, index, managed, false, inherited );
    }

    public SimplePluginRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                                     final ProjectVersionRef target, final int index, final boolean managed,
                                     final boolean reporting, final boolean inherited )
    {
        super( source, pomLocation, RelationshipType.PLUGIN, declaring, target, index, managed, inherited, false );
        this.reporting = reporting;
    }

    public SimplePluginRelationship( final Collection<URI> sources, final URI pomLocation,
                                     final ProjectVersionRef declaring, final ProjectVersionRef target, final int index,
                                     final boolean managed, final boolean reporting, final boolean inherited )
    {
        super( sources, pomLocation, RelationshipType.PLUGIN, declaring, target, index, managed, inherited, false );
        this.reporting = reporting;
    }

    public SimplePluginRelationship( final PluginRelationship relationship )
    {
        super( relationship );
        this.reporting = relationship.isReporting();
    }

    @Override
    public final boolean isReporting()
    {
        return reporting;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
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
        if ( !(obj instanceof PluginRelationship) )
        {
            return false;
        }
        final PluginRelationship other = (PluginRelationship) obj;
        return isManaged() == other.isManaged();
    }

    @Override
    public String toString()
    {
        return String.format( "PluginRelationship [%s => %s (managed=%s, index=%s)]", getDeclaring(), getTarget(),
                              isManaged(), getIndex() );
    }

    @Override
    protected ProjectVersionRef cloneTarget( final ProjectVersionRef target )
    {
        return new SimpleProjectVersionRef( target );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new SimpleArtifactRef( getTarget(), "maven-plugin", null );
    }

    @Override
    public PluginRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new SimplePluginRelationship( getSources(), getPomLocation(), ref, t, getIndex(), isManaged(), isReporting(), isInherited() );
    }

    @Override
    public PluginRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();

        return new SimplePluginRelationship( getSources(), getPomLocation(), d, ref, getIndex(), isManaged(), isReporting(), isInherited() );
    }

    @Override
    public synchronized PluginRelationship cloneFor( final ProjectVersionRef projectRef )
    {
        return new SimplePluginRelationship( getSources(), getPomLocation(), projectRef, getTarget(), getIndex(),
                                             isManaged(), reporting, isInherited() );
    }

    @Override
    public PluginRelationship addSource( final URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimplePluginRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), getIndex(),
                                             isManaged(), reporting, isInherited() );
    }

    @Override
    public PluginRelationship addSources( final Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimplePluginRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), getIndex(),
                                             isManaged(), reporting, isInherited() );
    }
}
