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

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.RelationshipIndex;

/**
 * Administrative access to the internal operations of the Neo4J driver without exposing it in public SPI or forcing a
 * type cast.
 * 
 * NOTE: Calling code should be managing a transaction somewhere up-stack already before anything here is called!
 *
 * @author jdcasey
 */
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
