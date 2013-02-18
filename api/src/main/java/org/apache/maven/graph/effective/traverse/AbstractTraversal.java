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
package org.apache.maven.graph.effective.traverse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.spi.GraphDriverException;

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
        throws GraphDriverException
    {
    }

    public int getRequiredPasses()
    {
        return passes;
    }

    public void endTraverse( final int pass, final EProjectNet network )
        throws GraphDriverException
    {
    }

    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                               final int pass )
    {
    }

    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        return true;
    }

    public TraversalType[] getTraversalTypes()
    {
        return types.toArray( new TraversalType[] {} );
    }
}
