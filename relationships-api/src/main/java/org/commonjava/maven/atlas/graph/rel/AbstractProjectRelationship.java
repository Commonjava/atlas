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

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.POM_ROOT_URI;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractProjectRelationship<T extends ProjectVersionRef>
    implements ProjectRelationship<T>, Serializable
{

    private static final long serialVersionUID = 1L;

    private final List<URI> sources = new ArrayList<URI>();

    private final RelationshipType type;

    private final ProjectVersionRef declaring;

    private final T target;

    private final int index;

    private boolean managed = false;

    @SuppressWarnings( "rawtypes" )
    private transient Constructor<? extends AbstractProjectRelationship> cloneCtor;

    private URI pomLocation;

    private final boolean cloneUsesLocation = true;

    protected AbstractProjectRelationship( final URI source, final RelationshipType type,
                                           final ProjectVersionRef declaring, final T target, final int index )
    {
        this( Collections.singleton( source ), POM_ROOT_URI, type, declaring, target, index, false );
    }

    protected AbstractProjectRelationship( final Collection<URI> sources, final RelationshipType type,
                                           final ProjectVersionRef declaring, final T target, final int index )
    {
        this( sources, POM_ROOT_URI, type, declaring, target, index, false );
    }

    protected AbstractProjectRelationship( final URI source, final RelationshipType type,
                                           final ProjectVersionRef declaring, final T target, final int index,
                                           final boolean managed )
    {
        this( Collections.singleton( source ), POM_ROOT_URI, type, declaring, target, index, managed );
    }

    protected AbstractProjectRelationship( final Collection<URI> sources, final RelationshipType type,
                                           final ProjectVersionRef declaring, final T target, final int index,
                                           final boolean managed )
    {
        this( sources, POM_ROOT_URI, type, declaring, target, index, managed );
    }

    protected AbstractProjectRelationship( final URI source, final URI pomLocation, final RelationshipType type,
                                           final ProjectVersionRef declaring, final T target, final int index )
    {
        this( Collections.singleton( source ), pomLocation, type, declaring, target, index, false );
    }

    protected AbstractProjectRelationship( final Collection<URI> sources, final URI pomLocation,
                                           final RelationshipType type, final ProjectVersionRef declaring,
                                           final T target, final int index )
    {
        this( sources, pomLocation, type, declaring, target, index, false );
    }

    protected AbstractProjectRelationship( final URI source, final URI pomLocation, final RelationshipType type,
                                           final ProjectVersionRef declaring, final T target, final int index,
                                           final boolean managed )
    {
        this( Collections.singleton( source ), pomLocation, type, declaring, target, index, managed );
    }

    protected AbstractProjectRelationship( final Collection<URI> sources, final URI pomLocation,
                                           final RelationshipType type, final ProjectVersionRef declaring,
                                           final T target, final int index, final boolean managed )
    {
        if ( sources == null )
        {
            throw new NullPointerException( "Source URIs cannot be null" );
        }

        for ( final URI source : sources )
        {
            addSource( source );
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
    }

    @Override
    public final boolean isManaged()
    {
        return managed;
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
    public abstract ProjectRelationship<T> cloneFor( final ProjectVersionRef declaring );

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
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final AbstractProjectRelationship<?> other = (AbstractProjectRelationship<?>) obj;
        if ( declaring == null )
        {
            if ( other.declaring != null )
            {
                return false;
            }
        }
        else if ( !declaring.equals( other.declaring ) )
        {
            return false;
        }
        if ( target == null )
        {
            if ( other.target != null )
            {
                return false;
            }
        }
        else if ( !target.equals( other.target ) )
        {
            return false;
        }
        if ( type != other.type )
        {
            return false;
        }
        return true;
    }

    @Override
    public final void addSource( final URI source )
    {
        if ( source == null )
        {
            return;
        }

        if ( !sources.contains( source ) )
        {
            this.sources.add( source );
        }
    }

    @Override
    public final void addSources( final Collection<URI> sources )
    {
        if ( sources == null )
        {
            return;
        }

        for ( final URI source : sources )
        {
            if ( source == null )
            {
                continue;
            }

            addSource( source );
        }
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
