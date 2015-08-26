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
package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoArtifactRef;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoProjectRef;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoProjectVersionRef;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoVersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.jackson.ProjectRefSerializer;
import org.commonjava.maven.atlas.ident.jackson.SerializerIdentityUtils;
import org.commonjava.maven.atlas.ident.ref.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NeoSpecificProjectVersionRefSerializerModule
        extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    private static final Set<Class<? extends ProjectRef>> REF_CLASSES;

    static
    {
        REF_CLASSES = Collections.unmodifiableSet( new HashSet<Class<? extends ProjectRef>>(
                Arrays.asList( NeoProjectRef.class, NeoProjectVersionRef.class, NeoArtifactRef.class,
                               NeoVersionlessArtifactRef.class ) ) );
    }

    public static final NeoSpecificProjectVersionRefSerializerModule INSTANCE = new NeoSpecificProjectVersionRefSerializerModule();

    public NeoSpecificProjectVersionRefSerializerModule()
    {
        super( "ProjectRef (with variants) Serializer" );

        for ( Class<? extends ProjectRef> cls: REF_CLASSES )
        {
            register( cls );
        }
    }

    private <T extends ProjectRef> void register( Class<T> cls )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Registering {} serializers", cls.getSimpleName() );

        addSerializer( cls, new ProjectRefSerializer<T>( cls, false ) );
        addKeySerializer( cls, new ProjectRefSerializer<T>( cls, true ) );
//
//        addDeserializer( cls, new ProjectRefDeserializer<T>( cls ) );
//        addKeyDeserializer( cls, new ProjectRefKeyDeserializer<T>( cls ) );
    }

    @Override
    public int hashCode()
    {
        return getClass().getSimpleName().hashCode() + 17;
    }

    @Override
    public boolean equals( final Object other )
    {
        return getClass().equals( other.getClass() );
    }

}
