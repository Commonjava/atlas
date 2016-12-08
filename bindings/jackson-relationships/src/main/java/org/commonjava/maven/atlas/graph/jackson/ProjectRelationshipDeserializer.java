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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.commonjava.maven.atlas.graph.jackson.SerializationConstants.*;

/**
 * Created by jdcasey on 8/26/15.
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
public final class ProjectRelationshipDeserializer<T extends ProjectRelationship>
        extends StdDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    public ProjectRelationshipDeserializer()
    {
        super( ProjectRelationship.class );
    }

    @Override
    public T deserialize( final JsonParser jp, final DeserializationContext ctx )
            throws JsonProcessingException, IOException
    {
        Map<String, Object> ast = new HashMap<String, Object>();
        Map<String, JsonLocation> locations = new HashMap<String, JsonLocation>();

        JsonToken token = jp.getCurrentToken();
        String currentField = null;
        List<String> currentArry = null;

        Logger logger = LoggerFactory.getLogger( getClass() );
        do
        {
            //                logger.info( "Token: {}", token );
            switch ( token )
            {
                case START_ARRAY:
                {
                    //                        logger.info( "Starting array for field: {}", currentField );
                    currentArry = new ArrayList<String>();
                    break;
                }
                case END_ARRAY:
                    //                        logger.info( "Ending array for field: {}", currentField );
                    locations.put( currentField, jp.getCurrentLocation() );
                    ast.put( currentField, currentArry );
                    currentArry = null;
                    break;
                case FIELD_NAME:
                    currentField = jp.getCurrentName();
                    break;
                case VALUE_STRING:
                    if ( currentArry != null )
                    {
                        currentArry.add( jp.getText() );
                    }
                    else
                    {
                        locations.put( currentField, jp.getCurrentLocation() );
                        ast.put( currentField, jp.getText() );
                    }
                    break;
                case VALUE_NUMBER_INT:
                    locations.put( currentField, jp.getCurrentLocation() );
                    ast.put( currentField, jp.getIntValue() );
                    break;
                case VALUE_NUMBER_FLOAT:
                    locations.put( currentField, jp.getCurrentLocation() );
                    ast.put( currentField, jp.getFloatValue() );
                    break;
                case VALUE_TRUE:
                    locations.put( currentField, jp.getCurrentLocation() );
                    ast.put( currentField, Boolean.TRUE );
                    break;
                case VALUE_FALSE:
                    locations.put( currentField, jp.getCurrentLocation() );
                    ast.put( currentField, Boolean.FALSE );
                    break;
            }

            token = jp.nextToken();
        }
        while ( token != JsonToken.END_OBJECT );

        StringBuilder sb = new StringBuilder();
        sb.append( "AST is:" );
        for ( String field : ast.keySet() )
        {
            Object value = ast.get( field );
            sb.append( "\n  " ).append( field ).append( " = " );
            if ( value == null )
            {
                sb.append( "null" );
            }
            else
            {
                sb.append( value ).append( "  (type: " ).append( value.getClass().getSimpleName() ).append( ")" );
            }
        }

        logger.debug( sb.toString() );

        final RelationshipType type = RelationshipType.getType( (String) ast.get( RELATIONSHIP_TYPE ) );

        final String uri = (String) ast.get( POM_LOCATION_URI );
        URI pomLocation;
        if ( uri == null )
        {
            pomLocation = RelationshipConstants.POM_ROOT_URI;
        }
        else
        {
            try
            {
                pomLocation = new URI( uri );
            }
            catch ( final URISyntaxException e )
            {
                throw new JsonParseException( "Invalid " + POM_LOCATION_URI + ": '" + uri + "': " + e.getMessage(),
                                              locations.get( POM_LOCATION_URI ), e );
            }
        }

        Collection<URI> sources = new HashSet<URI>();
        List<String> srcs = (List<String>) ast.get( SOURCE_URIS );
        if ( srcs != null )
        {
            for ( String u : srcs )
            {
                try
                {
                    sources.add( new URI( u ) );
                }
                catch ( URISyntaxException e )
                {
                    throw new JsonParseException( "Failed to parse source URI: " + u,
                                                  locations.get( SOURCE_URIS ) );
                }
            }
        }

        String decl = (String) ast.get( DECLARING_REF );
        final ProjectVersionRef declaring = SimpleProjectVersionRef.parse( decl );

        String tgt = (String) ast.get( TARGET_REF );
        Integer index = (Integer) ast.get( INDEX );
        if ( index == null )
        {
            index = 0;
        }

        // handle null implicitly by comparing to true.
        boolean managed = Boolean.TRUE.equals( ast.get( MANAGED ) );
        boolean inherited = Boolean.TRUE.equals( ast.get( INHERITED ) );
        boolean mixin = Boolean.TRUE.equals( ast.get( MIXIN ) );
        boolean optional = Boolean.TRUE.equals( ast.get( OPTIONAL ) );

        ProjectRelationship<?, ?> rel = null;
        switch ( type )
        {
            case DEPENDENCY:
            {
                final ArtifactRef target = SimpleArtifactRef.parse( tgt );

                String scp = (String) ast.get( SCOPE );
                final DependencyScope scope;
                if ( scp == null )
                {
                    scope = DependencyScope.compile;
                }
                else
                {
                    scope = DependencyScope.getScope( scp );
                }

                rel = new SimpleDependencyRelationship( sources, pomLocation, declaring, target, scope, index,
                                                        managed, inherited, optional );
                break;
            }
            case EXTENSION:
            {
                final ProjectVersionRef target = SimpleProjectVersionRef.parse( tgt );

                rel = new SimpleExtensionRelationship( sources, pomLocation, declaring, target, index, inherited );
                break;
            }
            case PARENT:
            {
                final ProjectVersionRef target = SimpleProjectVersionRef.parse( tgt );

                rel = new SimpleParentRelationship( sources, declaring, target );
                break;
            }
            case PLUGIN:
            {
                final ProjectVersionRef target = SimpleProjectVersionRef.parse( tgt );

                Boolean report = (Boolean) ast.get( REPORTING );
                rel = new SimplePluginRelationship( sources, pomLocation, declaring, target, index, managed,
                                                    Boolean.TRUE.equals( report ), inherited );
                break;
            }
            case PLUGIN_DEP:
            {
                String plug = (String) ast.get( PLUGIN_REF );
                if ( plug == null )
                {
                    throw new JsonParseException( "No plugin reference (field: " + PLUGIN_REF
                                                          + ") found in plugin-dependency relationship!",
                                                  jp.getCurrentLocation() );
                }

                final ProjectRef plugin = SimpleProjectRef.parse( plug );
                final ArtifactRef target = SimpleArtifactRef.parse( tgt );

                rel = new SimplePluginDependencyRelationship( sources, pomLocation, declaring, plugin, target,
                                                              index, managed, inherited );
                break;
            }
            case BOM:
            {
                final ProjectVersionRef target = SimpleProjectVersionRef.parse( tgt );

                rel = new SimpleBomRelationship( sources, pomLocation, declaring, target, index, inherited, mixin );
                break;
            }
        }

        return (T) rel;
    }
}
