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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.rel.RelationshipPathComparator;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Comparator;
import java.util.Iterator;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.POM_ROOT_URI;

public final class PathComparator
        implements Comparator<Path>
{

    public static final PathComparator INSTANCE = new PathComparator();

    private final RelationshipPathComparator pathComparator = RelationshipPathComparator.INSTANCE;

    private final ConversionCache cache = new ConversionCache();

    private PathComparator()
    {
    }

    @Override
    public int compare( final Path first, final Path second )
    {
        final int commonLen = Math.min( first.length(), second.length() );

        if ( first.length() > commonLen )
        {
            return 1;
        }
        else if ( second.length() > commonLen )
        {
            return -1;
        }

        Iterator<Relationship> firstRels = first.relationships().iterator();
        Iterator<Relationship> secondRels = second.relationships().iterator();
        for ( int i = 0; i < commonLen; i++ )
        {
            Relationship f = firstRels.next();
            Relationship s = secondRels.next();

            final int result = compareTypes( f, s );
            if ( result != 0 )
            {
                return result;
            }
        }

        firstRels = first.relationships().iterator();
        secondRels = second.relationships().iterator();
        for ( int i = 0; i < commonLen; i++ )
        {
            Relationship f = firstRels.next();
            Relationship s = secondRels.next();

            final int result = compareRels( f, s );
            if ( result != 0 )
            {
                return result;
            }
        }

        return 0;
    }

    private int compareTypes( Relationship f, Relationship s )
    {
        GraphRelType ft = GraphRelType.valueOf( f.getType().name() );
        GraphRelType st = GraphRelType.valueOf( s.getType().name() );
        return ft.ordinal() - st.ordinal();
    }

    private int compareRels( Relationship first, Relationship second )
    {
        if ( first.getType() == second.getType() )
        {
            String firstSrc = Conversions.getStringProperty( Conversions.POM_LOCATION_URI, first );
            String secSrc = Conversions.getStringProperty( Conversions.POM_LOCATION_URI, second );

            if ( firstSrc.equals( POM_ROOT_URI ) && !secSrc.equals( POM_ROOT_URI ) )
            {
                return -1;
            }
            else if ( !firstSrc.equals( POM_ROOT_URI ) && secSrc.equals( POM_ROOT_URI ) )
            {
                return 1;
            }

            if ( first.getEndNode().getId() == second.getStartNode().getId() )
            {
                return -1;
            }
            else if ( first.getStartNode().getId() == second.getEndNode().getId() )
            {
                return 1;
            }
            else if ( first.getStartNode().getId() == second.getStartNode().getId() )
            {
                return Conversions.getIntegerProperty( Conversions.INDEX, first ) - Conversions.getIntegerProperty(
                        Conversions.INDEX, second );
            }
        }
        else
        {
            // really, we can't reach this because of the way the main compare method works...
            return compareTypes( first, second );
        }

        return 0;
    }

}
