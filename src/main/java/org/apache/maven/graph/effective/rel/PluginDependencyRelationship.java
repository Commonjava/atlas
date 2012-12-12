/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective.rel;

import java.io.Serializable;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class PluginDependencyRelationship
    extends AbstractProjectRelationship<ArtifactRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final ProjectRef plugin;

    private final boolean managed;

    public PluginDependencyRelationship( final ProjectVersionRef declaring, final ProjectRef plugin,
                                         final ArtifactRef target, final int index, final boolean managed )
    {
        super( RelationshipType.PLUGIN_DEP, declaring, target, index );
        this.plugin = plugin;
        this.managed = managed;
    }

    public final ProjectRef getPlugin()
    {
        return plugin;
    }

    public final boolean isManaged()
    {
        return managed;
    }

    @Override
    public synchronized ProjectRelationship<ArtifactRef> cloneFor( final ProjectVersionRef projectRef )
    {
        return new PluginDependencyRelationship( projectRef, plugin, getTarget(), getIndex(), managed );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( managed ? 1231 : 1237 );
        result = prime * result + ( ( plugin == null ) ? 0 : plugin.hashCode() );
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
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final PluginDependencyRelationship other = (PluginDependencyRelationship) obj;
        if ( managed != other.managed )
        {
            return false;
        }
        if ( plugin == null )
        {
            if ( other.plugin != null )
            {
                return false;
            }
        }
        else if ( !plugin.equals( other.plugin ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "PluginDependencyRelationship [%s(%s) => %s (managed=%s, index=%s)]", getDeclaring(),
                              plugin, getTarget(), managed, getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

}
