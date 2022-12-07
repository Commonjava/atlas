/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.ident.jackson;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleVersionlessArtifactRef;
import org.commonjava.atlas.maven.ident.ref.VersionlessArtifactRef;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jdcasey on 8/26/15.
 */
public final class SerializerIdentityUtils
{
    private SerializerIdentityUtils(){}

    public static <T extends ProjectRef> T parse( final String value, final Class<T> type )
            throws IOException
    {
        Class<?> realType = null;
        if ( ArtifactRef.class.isAssignableFrom( type ) )
        {
            realType = SimpleArtifactRef.class;
        }
        else if ( VersionlessArtifactRef.class.isAssignableFrom( type ) )
        {
            realType = SimpleVersionlessArtifactRef.class;
        }
        else if ( ProjectVersionRef.class.isAssignableFrom( type ) )
        {
            realType = SimpleProjectVersionRef.class;
        }
        else if ( ProjectRef.class.isAssignableFrom( type ) )
        {
            realType = SimpleProjectRef.class;
        }
        else
        {
            throw new IOException( "Cannot find acceptable deserialization target class to parse: " + type.getSimpleName() );
        }

        try
        {
            final Method parseMethod = realType.getMethod( "parse", String.class );
            return type.cast( parseMethod.invoke( null, value ) );
        }
        catch ( final NoSuchMethodException e )
        {
            throw new IOException( "Failed to lookup/invoke parse() method on " + type.getSimpleName(), e );
        }
        catch ( final IllegalAccessException e )
        {
            throw new IOException( "Failed to lookup/invoke parse() method on " + type.getSimpleName(), e );
        }
        catch ( final InvocationTargetException e )
        {
            throw new IOException( "Failed to lookup/invoke parse() method on " + type.getSimpleName(), e );
        }
    }

}
