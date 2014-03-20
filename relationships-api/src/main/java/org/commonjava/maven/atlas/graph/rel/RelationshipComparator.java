/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.rel;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.POM_ROOT_URI;

import java.util.Comparator;

public final class RelationshipComparator
    implements Comparator<ProjectRelationship<?>>
{

    public static final RelationshipComparator INSTANCE = new RelationshipComparator();

    private RelationshipComparator()
    {
    }

    @Override
    public int compare( final ProjectRelationship<?> one, final ProjectRelationship<?> two )
    {
        if ( one.getType() == two.getType() )
        {
            if ( one.getPomLocation()
                    .equals( POM_ROOT_URI ) && !two.getPomLocation()
                                                   .equals( POM_ROOT_URI ) )
            {
                return -1;
            }
            else if ( !one.getPomLocation()
                          .equals( POM_ROOT_URI ) && two.getPomLocation()
                                                        .equals( POM_ROOT_URI ) )
            {
                return 1;
            }

            if ( one.getTarget()
                    .asProjectVersionRef()
                    .equals( two.getDeclaring() ) )
            {
                return -1;
            }
            else if ( one.getDeclaring()
                         .equals( two.getTarget()
                                     .asProjectVersionRef() ) )
            {
                return 1;
            }
            else if ( one.getDeclaring()
                         .equals( two.getDeclaring() ) )
            {
                return one.getIndex() - two.getIndex();
            }
        }
        else
        {
            return one.getType()
                      .ordinal() - two.getType()
                                      .ordinal();
        }

        return 0;
    }

}
