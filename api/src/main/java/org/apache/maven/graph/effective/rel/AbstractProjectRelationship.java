/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.apache.maven.graph.effective.rel;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public abstract class AbstractProjectRelationship<T extends ProjectVersionRef>
    implements ProjectRelationship<T>, Serializable
{

    private static final long serialVersionUID = 1L;

    private final RelationshipType type;

    private final ProjectVersionRef declaring;

    private final T target;

    private final int index;

    private boolean managed = false;

    @SuppressWarnings( "rawtypes" )
    private transient Constructor<? extends AbstractProjectRelationship> cloneCtor;

    protected AbstractProjectRelationship( final RelationshipType type, final ProjectVersionRef declaring,
                                           final T target, final int index )
    {
        this( type, declaring, target, index, false );
    }

    protected AbstractProjectRelationship( final RelationshipType type, final ProjectVersionRef declaring,
                                           final T target, final int index, final boolean managed )
    {
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

    public final boolean isManaged()
    {
        return managed;
    }

    public final int getIndex()
    {
        return index;
    }

    public final RelationshipType getType()
    {
        return type;
    }

    public final ProjectVersionRef getDeclaring()
    {
        return declaring;
    }

    public final T getTarget()
    {
        return target;
    }

    public abstract ArtifactRef getTargetArtifact();

    @SuppressWarnings( "unchecked" )
    public synchronized ProjectRelationship<T> cloneFor( final ProjectVersionRef projectRef )
    {
        if ( cloneCtor == null )
        {
            try
            {
                cloneCtor = getClass().getConstructor( ProjectVersionRef.class, target.getClass() );
            }
            catch ( final NoSuchMethodException e )
            {
                throw new IllegalArgumentException( "Missing constructor: " + getClass().getName()
                    + "(VersionedProjectRef declaring, " + target.getClass()
                                                                 .getName() + " target)", e );
            }
        }

        try
        {
            return cloneCtor.newInstance( projectRef, target );
        }
        catch ( final InstantiationException e )
        {
            throw new IllegalArgumentException( "Failed to create clone of: " + getClass().getName() + " for project: "
                + projectRef + ": " + e.getMessage(), e );
        }
        catch ( final IllegalAccessException e )
        {
            throw new IllegalArgumentException( "Failed to create clone of: " + getClass().getName() + " for project: "
                + projectRef + ": " + e.getMessage(), e );
        }
        catch ( final InvocationTargetException e )
        {
            throw new IllegalArgumentException( "Failed to create clone of: " + getClass().getName() + " for project: "
                + projectRef + ": " + e.getTargetException()
                                       .getMessage(), e.getTargetException() );
        }
    }

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

}
