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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public class PathExistenceVisitor
    extends AbstractTraverseVisitor
{

    private final Node end;

    private boolean found = false;

    public PathExistenceVisitor( final Node end )
    {
        this.end = end;
    }

    public boolean isFound()
    {
        return found;
    }

    @Override
    public boolean isEnabledFor( final Path path )
    {
        return !found;
    }

    @Override
    public void includingChild( final Relationship child, final Neo4jGraphPath childPath, final GraphPathInfo childPathInfo, final Path parentPath )
    {
        final Node end = child.getEndNode();
        if ( this.end.getId() == end.getId() )
        {
            found = true;
        }
    }

}
