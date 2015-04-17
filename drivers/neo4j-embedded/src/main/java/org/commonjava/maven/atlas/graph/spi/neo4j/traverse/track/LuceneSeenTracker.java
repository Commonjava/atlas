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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class LuceneSeenTracker
    implements TraverseSeenTracker
{

    private static final String SEEN_RELS_PREFIX = "_seen_rels_";

    private static final String KEY = "seen_key";

    private final GraphAdmin admin;

    private final Index<Node> seen;

    private final Node viewNode;

    public LuceneSeenTracker( final ViewParams view, final Node viewNode, final GraphAdmin admin )
    {
        this.viewNode = viewNode;
        this.seen = admin.getNodeIndex( view.getShortId() + SEEN_RELS_PREFIX + System.currentTimeMillis() );
        this.admin = admin;
    }

    @Override
    public boolean hasSeen( final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        // TODO: This trims the path leading up to the cycle...is that alright??

        String key;
        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( graphPath, admin );
        if ( cyclePath != null )
        {
            key = cyclePath.getKey();
        }
        else
        {
            key = graphPath.getKey();
        }

        key += "#" + pathInfo.getKey();
        final IndexHits<Node> hits = seen.get( KEY, key );
        if ( hits.hasNext() )
        {
            return true;
        }

        final Transaction tx = admin.beginTransaction();
        try
        {
            seen.add( viewNode, KEY, key );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return false;
    }

    @Override
    public void traverseComplete()
    {
        final Transaction tx = admin.beginTransaction();
        try
        {
            seen.delete();
            tx.success();
        }
        finally
        {
            tx.finish();
        }

    }

}
