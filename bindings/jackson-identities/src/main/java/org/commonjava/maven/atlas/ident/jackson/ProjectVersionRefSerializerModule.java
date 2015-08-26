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
package org.commonjava.maven.atlas.ident.jackson;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ProjectVersionRefSerializerModule
        extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    private static final Set<Class<? extends ProjectRef>> REF_CLASSES;

    static
    {
        REF_CLASSES = Collections.unmodifiableSet( new HashSet<Class<? extends ProjectRef>>(
                Arrays.asList( ProjectRef.class, ProjectVersionRef.class, ArtifactRef.class,
                               VersionlessArtifactRef.class, SimpleProjectRef.class, SimpleProjectVersionRef.class,
                               SimpleArtifactRef.class, SimpleVersionlessArtifactRef.class ) ) );
    }

    public static final ProjectVersionRefSerializerModule INSTANCE = new ProjectVersionRefSerializerModule();

    public ProjectVersionRefSerializerModule()
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
        logger.debug( "Registering {} serializers/deserialers", cls.getSimpleName() );

        addSerializer( cls, new ProjectRefSerializer<T>( cls, false ) );
        addKeySerializer( cls, new ProjectRefSerializer<T>( cls, true ) );

        addDeserializer( cls, new ProjectRefDeserializer<T>( cls ) );
        addKeyDeserializer( cls, new ProjectRefKeyDeserializer<T>( cls ) );
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
