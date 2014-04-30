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

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.fixture.FileDriverFixture;
import org.commonjava.maven.atlas.tck.graph.traverse.BuildOrderTraversalTCK;
import org.junit.Rule;

public class FileBuildOrderTraversalTest
    extends BuildOrderTraversalTCK
{
    @Rule
    public FileDriverFixture fixture = new FileDriverFixture();

    @Override
    protected synchronized RelationshipGraphFactory graphFactory()
        throws Exception
    {
        return fixture.manager();
    }
}
