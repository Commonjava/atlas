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
package org.apache.maven.graph.effective.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.EProjectRelationships;
import org.apache.maven.graph.effective.EProjectWeb;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.AbstractFilteringTraversal;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public class FilteringGraphTransformer
    extends AbstractFilteringTraversal
    implements ProjectGraphTransformer
{

    private EProjectKey key;

    private final Set<ProjectRelationship<?>> relationships = new HashSet<ProjectRelationship<?>>();

    private Set<EProjectCycle> cycles;

    private EGraphDriver driver;

    public FilteringGraphTransformer( final ProjectRelationshipFilter filter )
    {
        super( filter );
    }

    public FilteringGraphTransformer( final ProjectRelationshipFilter filter, final EProjectKey key )
    {
        super( filter );
        this.key = key;
    }

    public EProjectNet getTransformedNetwork()
    {
        if ( cycles != null )
        {
            for ( final EProjectCycle cycle : new HashSet<EProjectCycle>( cycles ) )
            {
                for ( final ProjectRelationship<?> rel : cycle )
                {
                    if ( !relationships.contains( rel ) )
                    {
                        cycles.remove( cycle );
                        break;
                    }
                }
            }
        }

        if ( driver != null )
        {
            driver.restrictRelationshipMembership( relationships );
        }

        if ( key == null )
        {
            return new EProjectWeb( relationships, Collections.<EProjectRelationships> emptyList(), cycles, driver );
        }

        return new EProjectGraph( key, relationships, Collections.<EProjectRelationships> emptyList(), cycles, driver );
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
        relationships.add( relationship );
        return true;
    }

    protected final boolean addRelationship( final ProjectRelationship<?> relationship )
    {
        return relationships.add( relationship );
    }

    protected final boolean removeRelationship( final ProjectRelationship<?> relationship )
    {
        return relationships.remove( relationship );
    }

    @Override
    public void startTraverse( final int pass, final EProjectNet network )
        throws GraphDriverException
    {
        if ( pass == 0 )
        {
            if ( network instanceof EProjectGraph )
            {
                key = ( (EProjectGraph) network ).getKey();
            }

            this.driver = network.getDriver()
                                 .newInstance();

            this.cycles = new HashSet<EProjectCycle>();

            final Set<EProjectCycle> graphCycles = network.getCycles();
            if ( graphCycles != null && !graphCycles.isEmpty() )
            {
                this.cycles.addAll( graphCycles );
            }
        }

        super.startTraverse( pass, network );
    }

    public boolean isEmpty()
    {
        return relationships.isEmpty();
    }

}
