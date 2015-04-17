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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubPathsCollectingVisitor
    extends AbstractTraverseVisitor
    implements Iterable<Neo4jGraphPath>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<Long> viaNodes;

    private final Set<Neo4jGraphPath> paths = new HashSet<Neo4jGraphPath>();

    private final GraphAdmin admin;

    public SubPathsCollectingVisitor( final Set<Long> viaNodes, final GraphAdmin admin )
    {
        this.viaNodes = viaNodes;
        this.admin = admin;
    }

    @Override
    public Iterator<Neo4jGraphPath> iterator()
    {
        return paths.iterator();
    }

    public Set<Neo4jGraphPath> getSubPaths()
    {
        return paths;
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        if ( path.length() > 1 )
        {
            final Neo4jGraphPath gp = new Neo4jGraphPath( path );
            final long[] allRids = gp.getRelationshipIds();
            int i = 0;
            Relationship start = null;
            for ( ; i < allRids.length; i++ )
            {
                start = admin.getRelationship( allRids[i] );
                if ( viaNodes.contains( start.getEndNode()
                                             .getId() ) )
                {
                    logger.debug( "found via-node ending: {}", start );
                    break;
                }
            }

            if ( start != null && i < allRids.length - 1 )
            {
                final long[] rids = new long[allRids.length - i];
                System.arraycopy( allRids, i, rids, 0, rids.length );

                final Relationship last = admin.getRelationship( allRids[allRids.length - 1] );
                paths.add( new Neo4jGraphPath( start.getStartNode(), last.getEndNode(), rids ) );
            }
        }

        return true;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setUseSelections( false );
    }

}
