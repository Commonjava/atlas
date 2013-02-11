package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.Map;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface Neo4JEGraphDriver
    extends EGraphDriver
{

    ExecutionResult executeFrom( String cypher, ProjectVersionRef... roots )
        throws GraphDriverException;

    ExecutionResult executeFrom( String cypher, ProjectRelationship<?> rootRel )
        throws GraphDriverException;

    ExecutionResult execute( String cypher )
        throws GraphDriverException;

    ExecutionResult executeFrom( String cypher, Map<String, Object> params, ProjectVersionRef... roots )
        throws GraphDriverException;

    ExecutionResult executeFrom( String cypher, Map<String, Object> params, ProjectRelationship<?> rootRel )
        throws GraphDriverException;

    ExecutionResult execute( String cypher, Map<String, Object> params )
        throws GraphDriverException;

    Node getNode( ProjectVersionRef ref )
        throws GraphDriverException;

    Relationship getRelationship( ProjectRelationship<?> rel )
        throws GraphDriverException;

}
