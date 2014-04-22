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

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.slf4j.LoggerFactory;

final class CycleDetectionTraversal
    extends AbstractTraversal
{
    private final List<EProjectCycle> cycles = new ArrayList<EProjectCycle>();

    private final ProjectRelationship<?> rel;

    CycleDetectionTraversal( final ProjectRelationship<?> rel )
    {
        this.rel = rel;
    }

    public List<EProjectCycle> getCycles()
    {
        return cycles;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( rel.getDeclaring()
                .equals( rel.getTarget()
                            .asProjectVersionRef() ) )
        {
            return false;
        }

        LoggerFactory.getLogger( getClass() )
                     .debug( "Checking for cycle: {}\n\nPath: {}\n\n", relationship, new JoinString( "\n", path ) );

        final ProjectVersionRef from = rel.getDeclaring();
        if ( from.equals( relationship.getTarget()
                                      .asProjectVersionRef() ) )
        {
            final List<ProjectRelationship<?>> cycle = new ArrayList<ProjectRelationship<?>>( path );
            cycle.add( rel );

            cycles.add( new EProjectCycle( cycle ) );
            return false;
        }

        return true;
    }
}
