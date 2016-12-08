/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.*;

import static org.commonjava.maven.atlas.graph.rel.RelationshipConstants.POM_ROOT_URI;

public abstract class AbstractSimpleProjectRelationship<R extends ProjectRelationship<R, T>, T extends ProjectVersionRef>
    implements ProjectRelationship<R, T>, Serializable
{

    private static final long serialVersionUID = 1L;

    private final List<URI> sources = new ArrayList<URI>();

    private final RelationshipType type;

    private final ProjectVersionRef declaring;

    private final T target;

    private final int index;

    private boolean managed = false;

    private boolean inherited;

    private boolean mixin;

    @SuppressWarnings( "rawtypes" )
    private transient Constructor<? extends AbstractSimpleProjectRelationship> cloneCtor;

    private URI pomLocation;

    private final boolean cloneUsesLocation = true;

    protected AbstractSimpleProjectRelationship( final URI source, final RelationshipType type,
                                                 final ProjectVersionRef declaring, final T target, final int index,
                                                 final boolean inherited, final boolean mixin )
    {
        this( Collections.singleton( source ), POM_ROOT_URI, type, declaring, target, index, false, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final Collection<URI> sources, final RelationshipType type,
                                                 final ProjectVersionRef declaring, final T target, final int index,
                                                 final boolean inherited, final boolean mixin )
    {
        this( sources, POM_ROOT_URI, type, declaring, target, index, false, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final URI source, final RelationshipType type,
                                                 final ProjectVersionRef declaring, final T target, final int index,
                                                 final boolean managed, final boolean inherited, final boolean mixin )
    {
        this( Collections.singleton( source ), POM_ROOT_URI, type, declaring, target, index, managed, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final Collection<URI> sources, final RelationshipType type,
                                                 final ProjectVersionRef declaring, final T target, final int index,
                                                 final boolean managed, final boolean inherited, final boolean mixin )
    {
        this( sources, POM_ROOT_URI, type, declaring, target, index, managed, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final URI source, final URI pomLocation, final RelationshipType type,
                                                 final ProjectVersionRef declaring, final T target, final int index,
                                                 final boolean inherited, final boolean mixin )
    {
        this( Collections.singleton( source ), pomLocation, type, declaring, target, index, false, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final Collection<URI> sources, final URI pomLocation,
                                                 final RelationshipType type, final ProjectVersionRef declaring,
                                                 final T target, final int index, final boolean inherited, final boolean mixin )
    {
        this( sources, pomLocation, type, declaring, target, index, false, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final URI source, final URI pomLocation, final RelationshipType type,
                                                 final ProjectVersionRef declaring, final T target, final int index,
                                                 final boolean managed, final boolean inherited, final boolean mixin )
    {
        this( Collections.singleton( source ), pomLocation, type, declaring, target, index, managed, inherited, mixin );
    }

    protected AbstractSimpleProjectRelationship( final Collection<URI> sources, final URI pomLocation,
                                                 final RelationshipType type, final ProjectVersionRef declaring,
                                                 final T target, final int index, final boolean managed,
                                                 final boolean inherited, final boolean mixin )
    {
        if ( sources == null )
        {
            throw new NullPointerException( "Source URIs cannot be null" );
        }

        for ( URI u : sources )
        {
            if ( !this.sources.contains( u ) )
            {
                this.sources.add( u );
            }
        }

        this.pomLocation = pomLocation;
        if ( declaring == null || target == null )
        {
            throw new NullPointerException( "Neither declaring ref (" + declaring + ") nor target ref (" + target
                + ") can be null!" );
        }

        this.type = type;
        this.declaring = declaring;
        this.target = target;
        this.index = index;
        this.managed = managed;
        this.inherited = inherited;
        this.mixin = mixin;
    }

    public AbstractSimpleProjectRelationship( final ProjectRelationship<R, T> relationship )
    {
        this.sources.addAll( relationship.getSources() );
        this.declaring = new SimpleProjectVersionRef( relationship.getDeclaring() );
        this.pomLocation = relationship.getPomLocation();
        this.index = relationship.getIndex();
        this.managed = relationship.isManaged();
        this.inherited = relationship.isInherited();
        this.mixin = relationship.isMixin();
        this.type = relationship.getType();
        this.target = cloneTarget( relationship.getTarget() );
    }

    protected abstract T cloneTarget( T target );

    @Override
    public final boolean isManaged()
    {
        return managed;
    }

    @Override
    public final boolean isInherited()
    {
        return inherited;
    }

    @Override
    public final boolean isMixin()
    {
        return mixin;
    }

    @Override
    public final int getIndex()
    {
        return index;
    }

    @Override
    public final RelationshipType getType()
    {
        return type;
    }

    @Override
    public final ProjectVersionRef getDeclaring()
    {
        return declaring;
    }

    @Override
    public final T getTarget()
    {
        return target;
    }

    @Override
    public abstract ArtifactRef getTargetArtifact();

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( declaring == null ) ? 0 : declaring.hashCode() );
        result = prime * result + ( ( target == null ) ? 0 : target.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( !(obj instanceof ProjectRelationship) )
        {
            return false;
        }
        final ProjectRelationship<?, ?> other = (ProjectRelationship<?, ?>) obj;
        if ( declaring == null )
        {
            if ( other.getDeclaring() != null )
            {
                return false;
            }
        }
        else if ( !declaring.equals( other.getDeclaring() ) )
        {
            return false;
        }
        if ( target == null )
        {
            if ( other.getTarget() != null )
            {
                return false;
            }
        }
        else if ( !target.equals( other.getTarget() ) )
        {
            return false;
        }
        return type == other.getType();
    }

    @Override
    public final Set<URI> getSources()
    {
        return new HashSet<URI>( sources );
    }

    @Override
    public final URI getPomLocation()
    {
        return pomLocation;
    }

}
