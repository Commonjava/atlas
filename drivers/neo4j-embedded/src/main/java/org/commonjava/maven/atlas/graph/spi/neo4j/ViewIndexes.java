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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;

public class ViewIndexes
{

    private static final String REL_CACHE_PREFIX = "rel_cache_for_";

    private static final String NODE_CACHE_PREFIX = "node_cache_for_";

    //    private static final String SELECTION_CACHE_PREFIX = "selection_cache_for_";

    private final IndexManager indexMgr;

    private final ViewParams view;

    public ViewIndexes( final IndexManager indexMgr, final ViewParams view )
    {
        this.indexMgr = indexMgr;
        this.view = view;
    }

    public RelationshipIndex getCachedRelationships()
    {
        return indexMgr.forRelationships( REL_CACHE_PREFIX + view.getShortId() );
    }

    //    public RelationshipIndex getSelections()
    //    {
    //        return indexMgr.forRelationships( SELECTION_CACHE_PREFIX + view.getShortId() );
    //    }

    public Index<Node> getCachedNodes()
    {
        return indexMgr.forNodes( NODE_CACHE_PREFIX + view.getShortId() );
    }

    public void delete()
    {
        getCachedRelationships().delete();
        //        getSelections().delete();
        getCachedNodes().delete();
    }

}
