/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
