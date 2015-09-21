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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Created by jdcasey on 8/24/15.
 */
public abstract class AbstractNeoProjectRelationship<R extends AbstractNeoProjectRelationship<R, I, T>, I extends ProjectRelationship<I, T>, T extends ProjectVersionRef>
    implements ProjectRelationship<I, T>
{
    protected Relationship rel;

    private RelationshipType type;

    protected ProjectVersionRef declaring;

    private boolean dirty;

    protected T target;

    protected Set<URI> sources;

    protected AbstractNeoProjectRelationship( Relationship rel, RelationshipType type )
    {
        this.rel = rel;
        this.type = type;
    }

    protected R cloneDirtyState( R old )
    {
        this.dirty = old.isDirty();
        this.declaring = old.declaring;
        this.target = old.target;
        this.sources = old.sources;
        return (R) this;
    }

    protected R withDeclaring( ProjectVersionRef declaring )
    {
        this.declaring = declaring;
        this.dirty = true;
        return (R) this;
    }

    protected R withTarget( T target )
    {
        this.target = target;
        this.dirty = true;
        return (R) this;
    }

    protected R withSources( Set<URI> sources )
    {
        this.sources = sources;
        this.dirty = true;
        return (R) this;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    @Override
    public synchronized I cloneFor( final ProjectVersionRef projectRef )
    {
        return selectDeclaring( projectRef );
    }

    @Override
    public int getIndex()
    {
        return NeoIdentityUtils.getIntegerProperty( rel, Conversions.INDEX, null, 0 );
    }

    @Override
    public RelationshipType getType()
    {
        return type;
    }

    @Override
    public ProjectVersionRef getDeclaring()
    {
        return declaring == null ? new NeoProjectVersionRef( rel.getStartNode() ) : declaring;
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget().asPomArtifact();
    }

    @Override
    public boolean isManaged()
    {
        return NeoIdentityUtils.getBooleanProperty( rel, Conversions.IS_MANAGED, null, false );
    }

    @Override
    public Set<URI> getSources()
    {
        if ( sources != null )
        {
            return sources;
        }

        Set<URI> srcs = Conversions.getURISetProperty( Conversions.SOURCE_URI, rel, RelationshipUtils.UNKNOWN_SOURCE_URI );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug("Got source URIs from relationship: {} of: {}", rel, srcs );

        if ( srcs != null )
        {
            return srcs;
        }

        return Collections.emptySet();
    }

    @Override
    public URI getPomLocation()
    {
        URI pl = Conversions.getURIProperty( Conversions.POM_LOCATION_URI, rel, RelationshipUtils.POM_ROOT_URI );
        if ( pl != null )
        {
            return pl;
        }

        return RelationshipUtils.POM_ROOT_URI;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getDeclaring() == null ) ? 0 : getDeclaring().hashCode() );
        result = prime * result + ( ( getTarget() == null ) ? 0 : getTarget().hashCode() );
        result = prime * result + ( ( getType() == null ) ? 0 : getType().hashCode() );
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
        if ( getDeclaring() == null )
        {
            if ( other.getDeclaring() != null )
            {
                return false;
            }
        }
        else if ( !getDeclaring().equals( other.getDeclaring() ) )
        {
            return false;
        }
        if ( getTarget() == null )
        {
            if ( other.getTarget() != null )
            {
                return false;
            }
        }
        else if ( !getTarget().equals( other.getTarget() ) )
        {
            return false;
        }
        return getType() == other.getType();
    }

    public abstract I detach();

}
