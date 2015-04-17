/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.RelationshipIndex;

public interface GraphAdmin
{

    FileNeo4JGraphConnection getDriver();

    Relationship getRelationship( long rid );

    Relationship select( Relationship r, ViewParams view, Node viewNode, GraphPathInfo viewPathInfo,
                         Neo4jGraphPath viewPath );

    RelationshipIndex getRelationshipIndex( String name );

    Index<Node> getNodeIndex( String name );

    Transaction beginTransaction();

    boolean isSelection( Relationship r, Node viewNode );

}
