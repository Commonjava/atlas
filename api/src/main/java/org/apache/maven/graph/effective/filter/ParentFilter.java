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

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class ParentFilter
    extends AbstractTypedFilter
{

    private final boolean allowTerminalParent;

    public ParentFilter()
    {
        this( true );
    }

    public ParentFilter( final boolean allowTerminalParent )
    {
        super( RelationshipType.PARENT, true, false, true );
        this.allowTerminalParent = allowTerminalParent;
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        if ( allowTerminalParent || !( (ParentRelationship) rel ).isTerminus() )
        {
            return true;
        }

        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "PARENTS" );
        if ( allowTerminalParent )
        {
            sb.append( "[include logical terminal-parent]" );
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
