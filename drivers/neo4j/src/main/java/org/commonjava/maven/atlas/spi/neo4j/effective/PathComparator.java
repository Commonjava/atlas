package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.Comparator;
import java.util.List;

import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.neo4j.graphdb.Path;

public class PathComparator
    implements Comparator<Path>
{

    private final RelationshipPathComparator pathComparator = new RelationshipPathComparator();

    private final Neo4JEGraphDriver driver;

    public PathComparator( final Neo4JEGraphDriver driver )
    {
        this.driver = driver;
    }

    public int compare( final Path first, final Path second )
    {
        final List<ProjectRelationship<?>> firstRels = driver.convertToRelationships( first.relationships() );
        final List<ProjectRelationship<?>> secondRels = driver.convertToRelationships( second.relationships() );

        return pathComparator.compare( firstRels, secondRels );
    }

}
