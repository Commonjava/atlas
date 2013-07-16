/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.Map;

import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;
import org.commonjava.maven.atlas.spi.GraphDriverException;
import org.commonjava.maven.atlas.spi.effective.EGraphDriver;
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
