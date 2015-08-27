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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdcasey on 8/24/15.
 */
public final class NeoIdentityUtils
{

    private NeoIdentityUtils(){}

    public static String getStringProperty( PropertyContainer container, String named, String memVal )
    {
        if ( memVal != null )
        {
            return memVal;
        }

        String v = container == null ? null : Conversions.getStringProperty( named, container );
        if ( v == null )
        {
            throw new InvalidRefException( named + " cannot both be null!" );
        }

        return v;
    }

    public static String getStringProperty( PropertyContainer container, String named, String memVal, String defaultVal )
    {
//        Logger logger = LoggerFactory.getLogger( NeoIdentityUtils.class );
//        logger.debug( "Looking for property: {} in: {}.\nMemory value: {}\nDefault value: {}", named, container, memVal, defaultVal);
        if ( memVal == null )
        {
            String v = container == null ? null : Conversions.getStringProperty(named, container);
//            logger.debug( "From container: {}, value: {}", container, v );
            return v == null ? defaultVal : v;
        }

//        logger.debug("Returning memVal: {}", memVal);
        return memVal;
    }

    public static boolean getBooleanProperty( PropertyContainer container, String named, Boolean memVal, boolean defaultVal )
    {
        if ( memVal != null )
        {
            return memVal;
        }

        Boolean v = container == null ? null : Conversions.getBooleanProperty( named, container);
        return v == null ? defaultVal : v;
    }

    public static ArtifactRef newNeoArtifactRef( ProjectVersionRef ref, ArtifactRef target )
    {
        NeoTypeAndClassifier tc;
        Boolean optional;
        if ( target instanceof NeoArtifactRef )
        {
            tc = (NeoTypeAndClassifier) target.getTypeAndClassifier();
            optional = ( (NeoArtifactRef) target ).getDirtyOptionalFlag();
        }
        else
        {
            tc = new NeoTypeAndClassifier( target.getType(), target.getClassifier() );
            optional = target.isOptional();
        }
        return new NeoArtifactRef( ref, tc, optional );
    }

    public static int getIntegerProperty( PropertyContainer container, String named, Integer memVal, int defaultVal )
    {
        if ( memVal != null )
        {
            return memVal;
        }

        Integer v = container == null ? null : Conversions.getIntegerProperty( named, container);
        return v == null ? defaultVal : v;
    }
}
