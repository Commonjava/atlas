package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class ConversionCache
{

    private Map<Long, ProjectRelationship<?>> relationships;

    private Map<Long, ProjectVersionRef> gavs;

    private Map<String, Object> serializedObjects;

    public ProjectRelationship<?> getRelationship( final Relationship rel )
    {
        return relationships == null ? null : relationships.get( rel.getId() );
    }

    public ProjectRelationship<?> getRelationship( final long rid )
    {
        return relationships == null ? null : relationships.get( rid );
    }

    public void cache( final Relationship rel, final ProjectRelationship<?> r )
    {
        if ( relationships == null )
        {
            relationships = new HashMap<Long, ProjectRelationship<?>>();
        }

        relationships.put( rel.getId(), r );
    }

    public ProjectVersionRef getProjectVersionRef( final Node node )
    {
        return gavs == null ? null : gavs.get( node.getId() );
    }

    public ProjectVersionRef getProjectVersionRef( final long nid )
    {
        return gavs == null ? null : gavs.get( nid );
    }

    public void cache( final Node node, final ProjectVersionRef ref )
    {
        if ( gavs == null )
        {
            gavs = new HashMap<Long, ProjectVersionRef>();
        }

        gavs.put( node.getId(), ref );
    }

    public <T> T getSerializedObject( final byte[] data, final Class<T> type )
    {
        if ( serializedObjects != null )
        {
            final String key = DigestUtils.shaHex( data );
            final Object value = serializedObjects.get( key );
            if ( value != null )
            {
                return type.cast( value );
            }
        }

        return null;
    }

    public void cache( final byte[] data, final Object value )
    {
        if ( serializedObjects == null )
        {
            serializedObjects = new HashMap<String, Object>();
        }

        final String key = DigestUtils.shaHex( data );
        serializedObjects.put( key, value );
    }
}
