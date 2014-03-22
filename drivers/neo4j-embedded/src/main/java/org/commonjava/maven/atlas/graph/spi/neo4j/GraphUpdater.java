package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AbstractTraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.RelationshipIndex;

public class GraphUpdater
    extends AbstractTraverseVisitor
{

    private final Node configNode;

    private final CycleCacheUpdater cycleUpdater;

    private final ConversionCache cache = new ConversionCache();

    private final RelationshipIndex allCyclePathRels;

    private final Set<CyclePath> allSeenCycles = new HashSet<CyclePath>();

    private final GraphView globalView;

    private int cycleCount = 0;

    private Set<Node> startNodes;

    private final GraphMaintenance maint;

    private final Set<ProjectRelationship<?>> cycleRelationships = new HashSet<ProjectRelationship<?>>();

    public GraphUpdater( final GraphView globalView, final Node configNode, final GraphMaintenance maint, final RelationshipIndex allCyclePathRels,
                         final CycleCacheUpdater cycleUpdater )
    {
        this.globalView = globalView;
        this.configNode = configNode;
        this.maint = maint;
        this.allCyclePathRels = allCyclePathRels;
        this.cycleUpdater = cycleUpdater;
    }

    @Override
    public void cycleDetected( final Path path )
    {
        final CyclePath cpath = new CyclePath( path );
        final Relationship last = path.lastRelationship();

        final ProjectRelationship<?> rel = Conversions.toProjectRelationship( last, cache );
        if ( rel != null )
        {
            cycleRelationships.add( rel );
        }

        if ( cycleUpdater.cacheCycle( cpath, last, allCyclePathRels, null, null, globalView, configNode, allSeenCycles ) )
        {
            cycleCount++;
        }
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setAvoidCycles( false );
        collector.setConversionCache( cache );
    }

    public int getCycleCount()
    {
        return cycleCount;
    }

    public Set<Node> getStartNodes()
    {
        return startNodes;
    }

    public void processUpdates( final Map<Long, ProjectRelationship<?>> createdRelationshipsMap )
    {
        if ( createdRelationshipsMap == null || createdRelationshipsMap.isEmpty() )
        {
            return;
        }

        startNodes = new HashSet<Node>();
        for ( final Long rid : createdRelationshipsMap.keySet() )
        {
            final Relationship r = maint.getRelationship( rid );
            startNodes.add( r.getStartNode() );
        }
    }

    public Set<ProjectRelationship<?>> getCycleRelationships()
    {
        return cycleRelationships;
    }

}
