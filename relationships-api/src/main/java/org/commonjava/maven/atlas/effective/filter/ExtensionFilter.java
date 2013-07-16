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
package org.commonjava.maven.atlas.effective.filter;

import org.commonjava.maven.atlas.common.DependencyScope;
import org.commonjava.maven.atlas.common.RelationshipType;
import org.commonjava.maven.atlas.effective.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

// TODO: Do we need to consider excludes in the direct plugin-level dependency?
public class ExtensionFilter
    extends AbstractTypedFilter
{

    public ExtensionFilter()
    {
        super( RelationshipType.EXTENSION, RelationshipType.DEPENDENCY, false, true );
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        if ( parent instanceof ExtensionRelationship )
        {
            return new OrFilter( new DependencyFilter( DependencyScope.runtime ), new ParentFilter( false ) );
        }
        else
        {
            return new NoneFilter();
        }
    }

    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "EXTENSIONS" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
