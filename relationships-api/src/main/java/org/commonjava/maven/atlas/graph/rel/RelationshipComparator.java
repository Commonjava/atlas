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
