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
package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

// TODO: Do we need to consider excludes in the direct plugin-level dependency?
public class PluginDependencyFilter
    implements ProjectRelationshipFilter
{

    private final ProjectRef plugin;

    public PluginDependencyFilter( final PluginRelationship plugin )
    {
        this.plugin = plugin.getTarget()
                            .asProjectRef();
    }

    public boolean accept( final ProjectRelationship<?> rel )
    {
        if ( rel instanceof PluginDependencyRelationship )
        {
            final PluginDependencyRelationship pdr = (PluginDependencyRelationship) rel;
            return plugin.equals( pdr.getPlugin() );
        }

        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new DependencyFilter( DependencyScope.runtime );
    }

}
