/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.rel;

import java.util.Comparator;
import java.util.List;

public class RelationshipPathComparator
    implements Comparator<List<ProjectRelationship<?>>>
{

    public static RelationshipPathComparator INSTANCE = new RelationshipPathComparator();

    private final RelationshipComparator comp = RelationshipComparator.INSTANCE;

    private RelationshipPathComparator()
    {
    }

    @Override
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
