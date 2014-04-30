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
package org.commonjava.maven.atlas.graph.spi.jung.traverse;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.jung.JungWorkspaceFactory;
import org.commonjava.maven.atlas.tck.graph.traverse.AncestryTraversalTCK;

public class AncestryTraversalTest
    extends AncestryTraversalTCK
{
    private EGraphManager manager;

    @Override
    protected RelationshipGraphFactory graphFactory()
        throws Exception
    {
        if ( manager == null )
        {
            manager = new EGraphManager( new JungWorkspaceFactory() );
        }

        return manager;
    }

}
