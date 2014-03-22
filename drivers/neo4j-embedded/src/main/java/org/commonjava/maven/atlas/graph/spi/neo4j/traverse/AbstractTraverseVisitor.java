package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track.MemorySeenTracker;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.track.TraverseSeenTracker;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public abstract class AbstractTraverseVisitor
    implements TraverseVisitor
{

    private final TraverseSeenTracker seenTracker;

    private ConversionCache conversionCache;

    protected AbstractTraverseVisitor()
    {
        seenTracker = new MemorySeenTracker();
    }

    protected AbstractTraverseVisitor( final TraverseSeenTracker seenTracker )
    {
        this.seenTracker = seenTracker;
    }

    public void setConversionCache( final ConversionCache conversionCache )
    {
        this.conversionCache = conversionCache;
    }

    public ConversionCache getConversionCache()
    {
        return conversionCache;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
    }

    @Override
    public boolean isEnabledFor( final Path path )
    {
        return true;
    }

    @Override
    public void cycleDetected( final CyclePath path, final Relationship injector )
    {
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        return true;
    }

    @Override
    public void includingChild( final Relationship child, final Neo4jGraphPath childPath, final GraphPathInfo childPathInfo, final Path parentPath )
    {
    }

    @Override
    public boolean shouldAvoidRedundantPaths()
    {
        return true;
    }

    @Override
    public GraphPathInfo initializeGraphPathInfoFor( final Path path, final Neo4jGraphPath graphPath, final GraphView view )
    {
        // just starting out. Initialize the path info.
        return new GraphPathInfo( view );
    }

    @Override
    public Neo4jGraphPath spliceGraphPathFor( final Neo4jGraphPath graphPath, final Path path )
    {
        return graphPath;
    }

    @Override
    public GraphPathInfo spliceGraphPathInfoFor( final GraphPathInfo pathInfo, final Neo4jGraphPath graphPath, final Path path )
    {
        return pathInfo;
    }

    @Override
    public boolean hasSeen( final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        return seenTracker.hasSeen( graphPath, pathInfo );
    }

}
