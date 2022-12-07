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
package org.commonjava.atlas.maven.graph.jackson;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.commonjava.atlas.maven.graph.rel.DependencyRelationship;
import org.commonjava.atlas.maven.graph.rel.PluginDependencyRelationship;
import org.commonjava.atlas.maven.graph.rel.PluginRelationship;
import org.commonjava.atlas.maven.graph.rel.ProjectRelationship;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by jdcasey on 8/26/15.
 */
@SuppressWarnings( "rawtypes" )
public final class ProjectRelationshipSerializer<T extends ProjectRelationship>
        extends StdSerializer<T>
{
    public ProjectRelationshipSerializer( final Class<T> cls )
    {
        super( cls );
    }

    @SuppressWarnings( "incomplete-switch" )
    @Override
    public void serialize( final T value, final JsonGenerator gen, final SerializerProvider provider )
            throws IOException, JsonGenerationException
    {
        gen.writeStartObject();
        gen.writeStringField( SerializationConstants.RELATIONSHIP_TYPE, value.getType().name() );
        gen.writeStringField( SerializationConstants.POM_LOCATION_URI, value.getPomLocation().toString() );
        gen.writeBooleanField( SerializationConstants.INHERITED, value.isInherited() );

        Set<URI> sources = value.getSources();
        if ( sources != null )
        {
            for ( Iterator<URI> iter = sources.iterator(); iter.hasNext(); )
            {
                if ( iter.next() == null )
                {
                    iter.remove();
                }
            }
            if ( !sources.isEmpty() )
            {
                gen.writeArrayFieldStart( SerializationConstants.SOURCE_URIS );
                for ( URI uri : sources )
                {
                    if ( uri == null )
                    {
                        continue;
                    }
                    gen.writeString( uri.toString() );
                }
                gen.writeEndArray();
            }
        }
        provider.defaultSerializeField( SerializationConstants.DECLARING_REF, value.getDeclaring(), gen );
        provider.defaultSerializeField( SerializationConstants.TARGET_REF, value.getTarget(), gen );

        switch ( value.getType() )
        {
            case BOM:
                gen.writeBooleanField( SerializationConstants.MIXIN, value.isMixin() );
                break;
            case DEPENDENCY:
            {
                gen.writeStringField( SerializationConstants.SCOPE, ( (DependencyRelationship) value ).getScope().realName() );
                gen.writeBooleanField( SerializationConstants.MANAGED, value.isManaged() );
                gen.writeBooleanField( SerializationConstants.OPTIONAL, ( (DependencyRelationship) value ).isOptional() );
                break;
            }
            case PLUGIN_DEP:
            {
                provider.defaultSerializeField( SerializationConstants.PLUGIN_REF, ( (PluginDependencyRelationship) value ).getPlugin(),
                                                gen );
                gen.writeBooleanField( SerializationConstants.MANAGED, value.isManaged() );
                break;
            }
            case PLUGIN:
            {

                gen.writeBooleanField( SerializationConstants.MANAGED, value.isManaged() );
                gen.writeBooleanField( SerializationConstants.REPORTING, ( (PluginRelationship) value ).isReporting() );
                break;
            }
        }

        gen.writeNumberField( SerializationConstants.INDEX, value.getIndex() );
        gen.writeEndObject();
    }

}
