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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;

public abstract class AbstractTraversal
    implements ProjectNetTraversal
{

    private final List<TraversalType> types;

    private final int passes;

    protected AbstractTraversal()
    {
        this.passes = 1;
        this.types = Collections.singletonList( TraversalType.depth_first );
    }

    protected AbstractTraversal( final int passes, final TraversalType... types )
    {
        this.passes = passes;
        this.types = Arrays.asList( types );
    }

    protected AbstractTraversal( final TraversalType... types )
    {
        this.passes = 1;
        this.types = Arrays.asList( types );
    }

    public TraversalType getType( final int pass )
    {
        if ( types == null || types.isEmpty() )
        {
            return TraversalType.depth_first;
        }
        else if ( pass >= types.size() )
        {
            return types.get( types.size() - 1 );
        }

        return types.get( pass );
    }

    public void startTraverse( final int pass, final EProjectNet network )
        throws RelationshipGraphConnectionException
    {
    }

    public int getRequiredPasses()
    {
        return passes;
    }

    public void endTraverse( final int pass, final EProjectNet network )
        throws RelationshipGraphConnectionException
    {
    }

    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                               final int pass )
    {
    }

    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        return preCheck( relationship, path, pass );
    }

    public TraversalType[] getTraversalTypes()
    {
        return types.toArray( new TraversalType[] {} );
    }
}
