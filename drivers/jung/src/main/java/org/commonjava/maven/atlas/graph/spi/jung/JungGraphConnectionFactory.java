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
package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;

public class JungGraphConnectionFactory
    implements RelationshipGraphConnectionFactory
{

    private final Map<String, JungGraphConnection> connections = new HashMap<String, JungGraphConnection>();

    @Override
    public RelationshipGraphConnection openConnection( final String workspaceId, final boolean create )
        throws RelationshipGraphConnectionException
    {
        JungGraphConnection connection = connections.get( workspaceId );
        if ( connection == null && create )
        {
            connection = new JungGraphConnection( workspaceId );
            connections.put( workspaceId, connection );
        }

        return connection;
    }

    @Override
    public Set<String> listWorkspaces()
    {
        return connections.keySet();
    }

    @Override
    public void flush( final RelationshipGraphConnection connection )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public boolean delete( final String workspaceId )
        throws RelationshipGraphConnectionException
    {
        return connections.remove( workspaceId ) != null;
    }

    @Override
    public void close()
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public boolean exists( final String workspaceId )
    {
        return connections.containsKey( workspaceId );
    }

}
