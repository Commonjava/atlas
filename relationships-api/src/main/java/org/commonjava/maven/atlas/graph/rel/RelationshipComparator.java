/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.graph.rel;

import static org.commonjava.maven.atlas.graph.rel.RelationshipConstants.POM_ROOT_URI;

import java.util.Comparator;

public final class RelationshipComparator
    implements Comparator<ProjectRelationship<?, ?>>
{

    public static final RelationshipComparator INSTANCE = new RelationshipComparator();

    private RelationshipComparator()
    {
    }

    @Override
    public int compare( final ProjectRelationship<?, ?> one, final ProjectRelationship<?, ?> two )
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

            int res = one.getDeclaring().compareTo( two.getDeclaring() );
            if ( res == 0 )
            {
                res = one.getIndex() - two.getIndex();
            }

            return res;
        }
        else
        {
            return one.getType()
                      .ordinal() - two.getType()
                                      .ordinal();
        }
    }

}
