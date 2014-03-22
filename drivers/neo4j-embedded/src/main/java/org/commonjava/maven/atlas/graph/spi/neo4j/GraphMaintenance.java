package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Relationship;

public interface GraphMaintenance
{

    AbstractNeo4JEGraphDriver getDriver();

    Relationship getRelationship( long rid );

    Relationship select( Relationship r, GraphView view, GraphPathInfo viewPathInfo, Neo4jGraphPath viewPath );

}
