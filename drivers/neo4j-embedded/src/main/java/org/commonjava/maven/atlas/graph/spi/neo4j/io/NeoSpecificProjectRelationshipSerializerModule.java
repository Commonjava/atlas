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

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipDeserializer;
import org.commonjava.maven.atlas.graph.jackson.ProjectRelationshipSerializer;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdcasey on 8/26/15.
 */
public class NeoSpecificProjectRelationshipSerializerModule
    extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    private static final Set<Class<? extends ProjectRelationship>> REL_CLASSES;

    static
    {
        REL_CLASSES = Collections.unmodifiableSet( new HashSet<Class<? extends ProjectRelationship>>(
                Arrays.asList( NeoBomRelationship.class, NeoDependencyRelationship.class,
                               NeoExtensionRelationship.class, NeoParentRelationship.class,
                               NeoPluginDependencyRelationship.class, NeoPluginRelationship.class ) ) );
    }

    public static final NeoSpecificProjectRelationshipSerializerModule INSTANCE = new NeoSpecificProjectRelationshipSerializerModule();

    public NeoSpecificProjectRelationshipSerializerModule()
    {
        super( "ProjectRelationship<?> Serializer" );

        for ( Class<? extends ProjectRelationship> cls : REL_CLASSES )
        {
            register( cls );
        }
    }

    private <T extends ProjectRelationship> void register( Class<T> cls )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Registering serializer/deserializer for: {}", cls.getSimpleName() );

        addSerializer( cls, new ProjectRelationshipSerializer<T>( cls ) );
        addDeserializer( cls, new ProjectRelationshipDeserializer<T>() );
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
