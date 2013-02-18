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
