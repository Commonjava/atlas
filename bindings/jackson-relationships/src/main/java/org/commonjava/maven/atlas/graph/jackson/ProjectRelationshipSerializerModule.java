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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.core.*;
import org.commonjava.maven.atlas.graph.rel.BomRelationship;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.commonjava.maven.atlas.graph.jackson.SerializationConstants.*;

public class ProjectRelationshipSerializerModule
                extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public ProjectRelationshipSerializerModule()
    {
        super( "ProjectRelationship<?> Serializer" );
        addSerializer( ProjectRelationship.class, new ProjectRelationshipSerializer() );
        addDeserializer( ProjectRelationship.class, new ProjectRelationshipDeserializer() );
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

    @SuppressWarnings( "rawtypes" )
    private static final class ProjectRelationshipSerializer
                    extends StdSerializer<ProjectRelationship>
    {
        ProjectRelationshipSerializer()
        {
            super( ProjectRelationship.class );
        }

        @SuppressWarnings( "incomplete-switch" )
        @Override
        public void serialize( final ProjectRelationship value, final JsonGenerator gen,
                               final SerializerProvider provider )
                        throws IOException, JsonGenerationException
        {
            gen.writeStartObject();
            gen.writeStringField( RELATIONSHIP_TYPE.getValue(), value.getType().name() );
            gen.writeStringField( POM_LOCATION_URI.getValue(), value.getPomLocation().toString() );
            gen.writeArrayFieldStart( SOURCE_URIS.getValue() );

            Set<URI> sources = value.getSources();
            for ( URI uri : sources )
            {
                gen.writeString( uri.toString() );
            }
            gen.writeEndArray();
            provider.defaultSerializeField( DECLARING_REF.getValue(), value.getDeclaring(), gen );
            provider.defaultSerializeField( TARGET_REF.getValue(), value.getTarget(), gen );

            switch ( value.getType() )
            {
                case DEPENDENCY:
                {
                    gen.writeStringField( SCOPE.getValue(), ( (DependencyRelationship) value ).getScope().realName() );
                    gen.writeBooleanField( MANAGED.getValue(), value.isManaged() );
                    break;
                }
                case PLUGIN_DEP:
                {
                    provider.defaultSerializeField( PLUGIN_REF.getValue(),
                                                    ( (PluginDependencyRelationship) value ).getPlugin(), gen );
                    gen.writeBooleanField( MANAGED.getValue(), value.isManaged() );
                    break;
                }
                case PLUGIN:
                {

                    gen.writeBooleanField( MANAGED.getValue(), value.isManaged() );
                    gen.writeBooleanField( REPORTING.getValue(), ( (PluginRelationship) value ).isReporting() );
                    break;
                }
            }

            gen.writeNumberField( INDEX.getValue(), value.getIndex() );
            gen.writeEndObject();
        }

    }

    @SuppressWarnings( "rawtypes" )
    private static final class ProjectRelationshipDeserializer
                    extends StdDeserializer<ProjectRelationship>
    {
        private static final long serialVersionUID = 1L;

        ProjectRelationshipDeserializer()
        {
            super( ProjectRelationship.class );
        }

        @Override
        public ProjectRelationship deserialize( final JsonParser jp, final DeserializationContext ctx )
                        throws IOException, JsonProcessingException
        {
            final JsonDeserializer<Object> prDeser =
                            ctx.findRootValueDeserializer( ctx.getTypeFactory().constructType( ProjectRef.class ) );
            final JsonDeserializer<Object> pvrDeser = ctx.findRootValueDeserializer(
                            ctx.getTypeFactory().constructType( ProjectVersionRef.class ) );
            final JsonDeserializer<Object> arDeser =
                            ctx.findRootValueDeserializer( ctx.getTypeFactory().constructType( ArtifactRef.class ) );

            final RelationshipType type =
                            RelationshipType.getType( expectField( jp, RELATIONSHIP_TYPE ).nextTextValue() );

            final String uri = expectField( jp, POM_LOCATION_URI ).nextTextValue();
            URI pomLocation;
            try
            {
                pomLocation = new URI( uri );
            }
            catch ( final URISyntaxException e )
            {
                throw new JsonParseException( "Invalid " + POM_LOCATION_URI + ": '" + uri + "': " + e.getMessage(),
                                              jp.getCurrentLocation(), e );
            }

            Collection<URI> sources = new HashSet<URI>();
            expectField( jp, SOURCE_URIS );
            ff( jp, JsonToken.START_ARRAY );
            while ( jp.nextToken() != JsonToken.END_ARRAY && jp.getCurrentToken() != null )
            {
                String u = jp.getText();
                if ( u == null )
                {
                    continue;
                }

                try
                {
                    sources.add( new URI( u ) );
                }
                catch ( URISyntaxException e )
                {
                    throw new JsonParseException( "Failed to parse source URI: " + u, jp.getCurrentLocation() );
                }
            }

            expectField( jp, DECLARING_REF ).nextTextValue();
            final ProjectVersionRef declaring = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

            ProjectRelationship<?> rel = null;
            switch ( type )
            {
                case DEPENDENCY:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ArtifactRef target = (ArtifactRef) arDeser.deserialize( jp, ctx );

                    final DependencyScope scope = DependencyScope.getScope( expectField( jp, SCOPE ).nextTextValue() );

                    rel = new DependencyRelationship( sources, pomLocation, declaring, target, scope, getIndex( jp ),
                                                      isManaged( jp ) );
                    break;
                }
                case EXTENSION:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    rel = new ExtensionRelationship( sources, pomLocation, declaring, target, getIndex( jp ) );
                    break;
                }
                case PARENT:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    rel = new ParentRelationship( sources, declaring, target );
                    break;
                }
                case PLUGIN:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    rel = new PluginRelationship( sources, pomLocation, declaring, target, getIndex( jp ),
                                                  isManaged( jp ), isReporting( jp ) );
                    break;
                }
                case PLUGIN_DEP:
                {
                    expectField( jp, PLUGIN_REF ).nextTextValue();
                    final ProjectRef plugin = (ProjectRef) prDeser.deserialize( jp, ctx );

                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ArtifactRef target = (ArtifactRef) arDeser.deserialize( jp, ctx );

                    rel = new PluginDependencyRelationship( sources, pomLocation, declaring, plugin, target,
                                                            getIndex( jp ), isManaged( jp ) );
                    break;
                }
                case BOM:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    rel = new BomRelationship( sources, pomLocation, declaring, target, getIndex( jp ) );
                    break;
                }
            }

            while ( jp.getCurrentToken() != JsonToken.END_OBJECT )
            {
                jp.nextToken();
            }

            return rel;
        }

        private void ff( JsonParser jp, JsonToken token )
                        throws JsonParseException, IOException
        {
            if ( jp.getCurrentToken() == token )
            {
                return;
            }

            JsonToken t = null;
            do
            {
                t = jp.nextToken();
                if ( t == null )
                {
                    throw new JsonParseException( "Expected token: " + token, jp.getCurrentLocation() );
                }
            }
            while ( t != token );
        }

        private JsonParser expectField( final JsonParser jp, final SerializedString named )
                        throws JsonParseException, IOException
        {
            while ( jp.getCurrentToken() != null && !named.getValue().equals( jp.getCurrentName() ) )
            {
                jp.nextToken();
            }

            if ( !named.getValue().equals( jp.getCurrentName() ) )
            {
                throw new JsonParseException( "Expected field: " + named, jp.getCurrentLocation() );
            }

            return jp;
        }

        private boolean isManaged( final JsonParser jp )
                        throws IOException
        {
            return expectField( jp, MANAGED ).nextBooleanValue();
        }

        private int getIndex( final JsonParser jp )
                        throws IOException
        {
            return expectField( jp, INDEX ).nextIntValue( 0 );
        }

        private boolean isReporting( final JsonParser jp )
                        throws IOException
        {
            return expectField( jp, REPORTING ).nextBooleanValue();
        }

    }

}
