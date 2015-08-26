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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

import java.io.IOException;

/**
 * Created by jdcasey on 8/26/15.
 */
public final class ProjectRefSerializer<T extends ProjectRef>
        extends StdSerializer<T>
{
    private boolean keySer;

    public ProjectRefSerializer( final Class<T> refCls, boolean keySer )
    {
        super( refCls );
        this.keySer = keySer;
    }

    @Override
    public void serialize( final T src, final JsonGenerator generator, final SerializerProvider provider )
            throws IOException, JsonGenerationException
    {
        if ( keySer )
        {
            generator.writeFieldName( src.toString() );
        }
        else
        {
            generator.writeString( src.toString() );
        }
    }
}
