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

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.commonjava.maven.atlas.graph.model.PluginKey;
import org.commonjava.maven.atlas.graph.rel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProjectRelationshipSerializerModule
        extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    private static final Set<Class<? extends ProjectRelationship>> REL_CLASSES;

    static
    {
        REL_CLASSES = Collections.unmodifiableSet( new HashSet<Class<? extends ProjectRelationship>>(
                Arrays.asList( ProjectRelationship.class, BomRelationship.class, DependencyRelationship.class,
                               ExtensionRelationship.class, ParentRelationship.class,
                               PluginDependencyRelationship.class, PluginRelationship.class,
                               SimpleBomRelationship.class, SimpleDependencyRelationship.class,
                               SimpleExtensionRelationship.class, SimpleParentRelationship.class,
                               SimplePluginDependencyRelationship.class, SimplePluginRelationship.class ) ) );
    }

    public static final ProjectRelationshipSerializerModule INSTANCE = new ProjectRelationshipSerializerModule();

    public ProjectRelationshipSerializerModule()
    {
        super( "ProjectRelationship<?> Serializer" );

        for ( Class<? extends ProjectRelationship> cls : REL_CLASSES )
        {
            register( cls );
        }
        addKeySerializer( PluginKey.class, new PluginKeySerializer() );
        addKeyDeserializer( PluginKey.class, new PluginKeyDeserializer() );
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
