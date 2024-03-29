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
package org.commonjava.atlas.maven.graph.model;

import org.commonjava.atlas.maven.graph.rel.PluginRelationship;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by ruhan on 3/17/17.
 */
public final class PluginKey
        implements Serializable
{
    private String groupId;

    private String artifactId;

    private String version;

    private boolean managed;

    public PluginKey() {}

    public PluginKey(ProjectVersionRef target, boolean managed)
    {
        this.groupId = target.getGroupId();
        this.artifactId = target.getArtifactId();
        this.version = target.getVersionString();
        this.managed = managed;
    }

    public PluginKey(PluginRelationship rel)
    {
        this(rel.getTarget(), rel.isManaged());
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public boolean getManaged()
    {
        return managed;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s:%s", groupId, artifactId, version, managed );
    }

    public static PluginKey parse( final String pk )
    {
        final String[] parts = pk.split( ":" );
        if ( parts.length < 4 )
        {
            throw new RuntimeException( "PluginKey parse failed. (Given: '" + pk + "')" );
        }
        PluginKey ret = new PluginKey();
        ret.groupId = parts[0];
        ret.artifactId = parts[1];
        ret.version = parts[2];
        ret.managed = Boolean.parseBoolean(parts[3]);
        return ret;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof PluginKey))
        {
            return false;
        }

        final PluginKey other = (PluginKey) obj;

        if (groupId == null)
        {
            if (other.getGroupId() != null)
            {
                return false;
            }
        }
        else if (!groupId.equals(other.getGroupId()))
        {
            return false;
        }

        if (artifactId == null)
        {
            if (other.getArtifactId() != null)
            {
                return false;
            }
        }
        else if (!artifactId.equals(other.getArtifactId()))
        {
            return false;
        }

        if (version == null)
        {
            if (other.getVersion() != null)
            {
                return false;
            }
        }
        else if (!version.equals(other.getVersion()))
        {
            return false;
        }

        return managed == other.getManaged();
    }

}
