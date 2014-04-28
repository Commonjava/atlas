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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.Map;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.cypher.javacompat.ExecutionResult;

public interface Neo4JEGraphDriver
    extends RelationshipGraphConnection
{

    ExecutionResult executeFrom( String cypher, ProjectVersionRef... roots )
        throws RelationshipGraphConnectionException;

    ExecutionResult executeFrom( String cypher, ProjectRelationship<?> rootRel )
        throws RelationshipGraphConnectionException;

    ExecutionResult execute( String cypher )
        throws RelationshipGraphConnectionException;

    ExecutionResult executeFrom( String cypher, Map<String, Object> params, ProjectVersionRef... roots )
        throws RelationshipGraphConnectionException;

    ExecutionResult executeFrom( String cypher, Map<String, Object> params, ProjectRelationship<?> rootRel )
        throws RelationshipGraphConnectionException;

    ExecutionResult execute( String cypher, Map<String, Object> params )
        throws RelationshipGraphConnectionException;

    //    Node getNode( ProjectVersionRef ref )
    //        throws GraphDriverException;
    //
    //    Relationship getRelationship( ProjectRelationship<?> rel )
    //        throws GraphDriverException;

}
