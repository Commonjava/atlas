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
package org.commonjava.atlas.maven.graph.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.commonjava.atlas.maven.graph.model.PluginKey;

import java.io.IOException;

/**
 * Created by ruhan on 3/16/17.
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
public final class PluginKeyDeserializer
        extends KeyDeserializer
{
    public PluginKeyDeserializer() {}

    @Override
    public Object deserializeKey(String json, DeserializationContext ctx)
            throws IOException, JsonProcessingException
    {
        return PluginKey.parse( json );
    }
}
