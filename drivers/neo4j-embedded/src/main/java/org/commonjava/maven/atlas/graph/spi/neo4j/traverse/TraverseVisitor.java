package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public interface TraverseVisitor
{

    /**
     * Allow configuration of extra options for this collector as it initializes 
     * with this visitor.
     */
    void configure( AtlasCollector<?> collector );

    /**
     * MUST call this after traverse completes, to allow resource reclamation.
     */
    void traverseComplete( AtlasCollector<?> collector );

    /**
     * Allow visitor to turn itself off and skip further traversal of a path.
     */
    boolean isEnabledFor( Path path );

    /**
     * Handle detected cycle (which was traversed TO, but not THROUGH).
     */
    void cycleDetected( CyclePath cp, Relationship injector );

    /**
     * Whether child edges of the given path should be visited.
     */
    boolean includeChildren( Path path, Neo4jGraphPath graphPath, GraphPathInfo pathInfo );

    /**
     * Notice that a child relationship has been included.
     * 
     * @param child The relationship that WILL BE traversed
     * @param childPath The {@link Neo4jGraphPath} that will be associated with traversing this child
     * @param childPathInfo {@link GraphPathInfo} that will be associated with traversing this child
     * @param parentPath parent {@link Path} which will be extended by this child
     */
    void includingChild( Relationship child, Neo4jGraphPath childPath, GraphPathInfo childPathInfo, Path parentPath );

}
