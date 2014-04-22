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
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.cypher.javacompat.ExecutionResult;

public interface Neo4JEGraphDriver
    extends GraphDatabaseDriver
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

    //    Node getNode( ProjectVersionRef ref )
    //        throws GraphDriverException;
    //
    //    Relationship getRelationship( ProjectRelationship<?> rel )
    //        throws GraphDriverException;

}
