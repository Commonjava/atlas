/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
