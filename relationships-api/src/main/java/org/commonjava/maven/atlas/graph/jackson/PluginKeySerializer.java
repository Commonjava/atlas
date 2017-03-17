package org.commonjava.maven.atlas.graph.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.commonjava.maven.atlas.graph.model.PluginKey;

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
