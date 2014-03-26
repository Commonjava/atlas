package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.model.GraphView;
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

    private final GraphView view;

    public ViewIndexes( final IndexManager indexMgr, final GraphView view )
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
