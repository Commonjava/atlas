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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.Comparator;

import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Relationship;

public class AtlasRelIndexComparator
    implements Comparator<Relationship>
{

    @Override
    public int compare( final Relationship f, final Relationship s )
    {
        final int fidx = Conversions.getIntegerProperty( Conversions.ATLAS_RELATIONSHIP_INDEX, f, 0 );
        final int sidx = Conversions.getIntegerProperty( Conversions.ATLAS_RELATIONSHIP_INDEX, s, 0 );

        final int comp = fidx - sidx;
        if ( comp < 0 )
        {
            return -1;
        }
        else if ( comp > 0 )
        {
            return 1;
        }

        return comp;
    }

}
