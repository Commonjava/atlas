package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.RelationshipIndex;

public interface GraphAdmin
{

    AbstractNeo4JEGraphDriver getDriver();

    Relationship getRelationship( long rid );

    Relationship select( Relationship r, GraphView view, Node viewNode, GraphPathInfo viewPathInfo, Neo4jGraphPath viewPath );

    RelationshipIndex getRelationshipIndex( String name );

    Index<Node> getNodeIndex( String name );

    Transaction beginTransaction();

    boolean isSelection( Relationship r, Node viewNode );

}
