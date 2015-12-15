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
package org.commonjava.maven.atlas.graph.jackson;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Set;

import static org.commonjava.maven.atlas.graph.jackson.SerializationConstants.*;

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
        gen.writeStringField( RELATIONSHIP_TYPE, value.getType().name() );
        gen.writeStringField( POM_LOCATION_URI, value.getPomLocation().toString() );
        gen.writeBooleanField( INHERITED, value.isInherited() );

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
                gen.writeArrayFieldStart( SOURCE_URIS );
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
        provider.defaultSerializeField( DECLARING_REF, value.getDeclaring(), gen );
        provider.defaultSerializeField( TARGET_REF, value.getTarget(), gen );

        switch ( value.getType() )
        {
            case BOM:
                gen.writeBooleanField( MIXIN, value.isMixin() );
                break;
            case DEPENDENCY:
            {
                gen.writeStringField( SCOPE, ( (DependencyRelationship) value ).getScope().realName() );
                gen.writeBooleanField( MANAGED, value.isManaged() );
                gen.writeBooleanField( OPTIONAL, ( (DependencyRelationship) value ).isOptional() );
                break;
            }
            case PLUGIN_DEP:
            {
                provider.defaultSerializeField( PLUGIN_REF, ( (PluginDependencyRelationship) value ).getPlugin(),
                                                gen );
                gen.writeBooleanField( MANAGED, value.isManaged() );
                break;
            }
            case PLUGIN:
            {

                gen.writeBooleanField( MANAGED, value.isManaged() );
                gen.writeBooleanField( REPORTING, ( (PluginRelationship) value ).isReporting() );
                break;
            }
        }

        gen.writeNumberField( INDEX, value.getIndex() );
        gen.writeEndObject();
    }

}
