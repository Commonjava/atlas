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
package org.commonjava.atlas.maven.ident.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;

import java.io.IOException;

/**
 * Created by jdcasey on 8/26/15.
 */
public class ProjectRefDeserializer<T extends ProjectRef>
        extends StdDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    private final Class<T> refCls;

    public ProjectRefDeserializer( final Class<T> refCls )
    {
        super( refCls );
        this.refCls = refCls;
    }

    @Override
    public T deserialize( final JsonParser jp, final DeserializationContext ctxt )
            throws IOException, JsonProcessingException
    {
        return SerializerIdentityUtils.parse( jp.getText(), refCls );
    }
}
