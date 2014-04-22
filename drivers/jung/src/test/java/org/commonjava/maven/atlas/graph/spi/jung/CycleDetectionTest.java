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
package org.commonjava.maven.atlas.graph.spi.jung;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.tck.graph.CycleDetectionTCK;

public class CycleDetectionTest
    extends CycleDetectionTCK
{
    private EGraphManager manager;

    @Override
    protected EGraphManager getManager()
        throws Exception
    {
        if ( manager == null )
        {
            manager = new EGraphManager( new JungWorkspaceFactory() );
        }

        return manager;
    }

}
