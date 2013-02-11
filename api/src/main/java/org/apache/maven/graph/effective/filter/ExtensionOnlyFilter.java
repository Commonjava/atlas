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
import org.apache.maven.graph.effective.rel.ProjectRelationship;

// TODO: Do we need to consider excludes in the direct plugin-level dependency?
public class ExtensionOnlyFilter
    extends AbstractTypedFilter
{

    public ExtensionOnlyFilter()
    {
        super( RelationshipType.EXTENSION, false, false, true );
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new NoneFilter();
    }

    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "EXTENSIONS ONLY" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
