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
package org.commonjava.atlas.maven.graph.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.commonjava.atlas.maven.graph.model.PluginKey;

import java.io.IOException;

/**
 * Created by ruhan on 3/17/17.
 */
public class PluginKeySerializer
        extends JsonSerializer<PluginKey>
{

    @Override
    public void serialize(PluginKey pluginKey, JsonGenerator gen, SerializerProvider provider)
            throws IOException, JsonProcessingException
    {
        gen.writeFieldName( pluginKey.toString() );
    }
}
