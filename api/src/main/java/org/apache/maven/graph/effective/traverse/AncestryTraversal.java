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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class AncestryTraversal
    implements ProjectNetTraversal
{

    private final List<ProjectVersionRef> ancestry = new ArrayList<ProjectVersionRef>();

    public AncestryTraversal( final ProjectVersionRef startingFrom )
    {
        ancestry.add( startingFrom );
    }

    public AncestryTraversal()
    {
    }

    public List<ProjectVersionRef> getAncestry()
    {
        return Collections.unmodifiableList( ancestry );
    }

    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        if ( !preCheck( relationship, path, pass ) )
        {
            return false;
        }

        if ( ancestry.isEmpty() )
        {
            ancestry.add( relationship.getDeclaring() );
            ancestry.add( relationship.getTarget() );
            return true;
        }
        else if ( ancestry.get( ancestry.size() - 1 )
                          .equals( relationship.getDeclaring() ) )
        {
            ancestry.add( relationship.getTarget() );
            return true;
        }

        return false;
    }

    public boolean isInAncestry( final ProjectVersionRef ref )
    {
        return ancestry.contains( ref );
    }

    public TraversalType getType( final int pass )
    {
        return TraversalType.depth_first;
    }

    public void startTraverse( final int pass, final EProjectNet network )
    {
    }

    public int getRequiredPasses()
    {
        return 1;
    }

    public void endTraverse( final int pass, final EProjectNet network )
    {
    }

    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                               final int pass )
    {
    }

    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                             final int pass )
    {
        if ( relationship instanceof ParentRelationship && !relationship.getDeclaring()
                                                                        .equals( relationship.getTarget() ) )
        {
            return true;
        }

        return true;
    }

}
