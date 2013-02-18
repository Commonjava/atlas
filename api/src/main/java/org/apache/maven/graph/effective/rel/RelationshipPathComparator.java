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
package org.apache.maven.graph.effective.rel;

import java.util.Comparator;
import java.util.List;

public class RelationshipPathComparator
    implements Comparator<List<ProjectRelationship<?>>>
{

    private final RelationshipComparator comp = new RelationshipComparator();

    public int compare( final List<ProjectRelationship<?>> one, final List<ProjectRelationship<?>> two )
    {
        final int commonLen = Math.min( one.size(), two.size() );

        if ( one.size() > commonLen )
        {
            return 1;
        }
        else if ( two.size() > commonLen )
        {
            return -1;
        }

        for ( int i = 0; i < commonLen; i++ )
        {
            final int result = compareRelTypes( one.get( i ), two.get( i ) );
            if ( result != 0 )
            {
                return result;
            }
        }

        for ( int i = 0; i < commonLen; i++ )
        {
            final int result = compareRels( one.get( i ), two.get( i ) );
            if ( result != 0 )
            {
                return result;
            }
        }

        return 0;
    }

    private int compareRels( final ProjectRelationship<?> one, final ProjectRelationship<?> two )
    {
        return comp.compare( one, two );
    }

    private int compareRelTypes( final ProjectRelationship<?> one, final ProjectRelationship<?> two )
    {
        return one.getType()
                  .ordinal() - two.getType()
                                  .ordinal();
    }

}
