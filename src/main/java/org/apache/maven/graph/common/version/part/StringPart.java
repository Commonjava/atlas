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
package org.apache.maven.graph.common.version.part;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StringPart
    extends VersionPart
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private static final String ZERO_EQUIV = "";

    private static final String RANDOM_STRING_EQUIV = "_random";

    // FIXME: How do we want to do this??
    private static final List<String> MARKER_ORDER = new ArrayList<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add( "PREVIEW" );
            add( "MILESTONE" );
            add( "ALPHA" );
            add( "BETA" );
            add( "RC" );
            add( ZERO_EQUIV );
            add( "SP" );
            add( RANDOM_STRING_EQUIV );
        }
    };

    private static final int ZERO_EQUIV_INDEX = MARKER_ORDER.indexOf( ZERO_EQUIV );

    public static final int ADJ_ZERO_EQUIV_INDEX = ZERO_EQUIV_INDEX - MARKER_ORDER.size();

    private static final int RANDOM_STRING_EQUIV_INDEX = MARKER_ORDER.indexOf( RANDOM_STRING_EQUIV );

    public static final int ADJ_RANDOM_STRING_EQUIV_INDEX = RANDOM_STRING_EQUIV_INDEX - MARKER_ORDER.size();

    private static final Map<String, String> ALIASES = new HashMap<String, String>()
    {
        private static final long serialVersionUID = 1L;

        {
            put( "PRE", "PREVIEW" );
            put( "M", "MILESTONE" );
            put( "A", "ALPHA" );
            put( "B", "BETA" );
            put( "CR", "RC" ); // candidate for release == release candidate
            put( "GA", ZERO_EQUIV );
            put( "FINAL", ZERO_EQUIV );
            put( "CP", "SP" ); // cumulative patch == service pack
        }

    };

    private final String value;

    private final Integer zeroCompareIndex;

    public StringPart( final String value )
    {
        this.value = value;
        String uc = value.toUpperCase();
        if ( ALIASES.containsKey( uc ) )
        {
            uc = ALIASES.get( uc );
        }

        int idx = MARKER_ORDER.indexOf( uc );

        // if this isn't a standard marker, it always sorts AFTER a zero-equivalent segment.
        // otherwise, compare the standard marker's position releative to the zero placeholder in the marker-order list.
        if ( idx > -1 )
        {
            // now, adjust to make sure all values are negative.
            idx -= MARKER_ORDER.size();
        }
        else
        {
            idx = ADJ_RANDOM_STRING_EQUIV_INDEX;
        }

        zeroCompareIndex = idx;
    }

    public boolean isMarker()
    {
        return zeroCompareIndex != ADJ_RANDOM_STRING_EQUIV_INDEX;
    }

    public Integer getZeroCompareIndex()
    {
        return zeroCompareIndex;
    }

    @Override
    public String renderStandard()
    {
        return value;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.format( "STR[%s]", value );
    }

    public int compareTo( final VersionPart part )
    {
        if ( part instanceof SnapshotPart )
        {
            final Integer idx = zeroCompareIndex == null ? 1 : zeroCompareIndex;

            System.out.println( "my zIndex: " + idx + "\nadjusted zero-equiv. index: " + ADJ_ZERO_EQUIV_INDEX );

            return idx.compareTo( ADJ_ZERO_EQUIV_INDEX );
        }
        else if ( part instanceof NumericPart )
        {
            if ( !NumericPart.ZERO.equals( part ) )
            {
                return -1;
            }
            else
            {
                final Integer idx = zeroCompareIndex == null ? 1 : zeroCompareIndex;

                return idx.compareTo( ADJ_ZERO_EQUIV_INDEX );
            }
        }
        else if ( part instanceof StringPart )
        {
            final StringPart otherStr = (StringPart) part;

            final Integer zci = zeroCompareIndex;
            final Integer ozci = otherStr.getZeroCompareIndex();

            if ( zci == ADJ_RANDOM_STRING_EQUIV_INDEX && ozci == ADJ_RANDOM_STRING_EQUIV_INDEX )
            {
                return value.toLowerCase( Locale.ENGLISH )
                            .compareTo( otherStr.getValue()
                                                .toLowerCase( Locale.ENGLISH ) );
            }

            return zci.compareTo( ozci );
        }
        // 1.2-foo > 1.2-SNAPSHOT
        else if ( part instanceof SnapshotPart )
        {
            return 1;
        }

        // punt...shouldn't happen.
        return 0;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        // result = prime * result + ( ( value == null ) ? 0 : value.toLowerCase()
        // .hashCode() );
        result = prime * result + ( ( zeroCompareIndex == null ) ? 0 : zeroCompareIndex.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final StringPart other = (StringPart) obj;

        final Integer zci = zeroCompareIndex;
        final Integer ozci = other.getZeroCompareIndex();

        if ( zci == ADJ_RANDOM_STRING_EQUIV_INDEX && ozci == ADJ_RANDOM_STRING_EQUIV_INDEX )
        {
            if ( value == null )
            {
                if ( other.value != null )
                {
                    return false;
                }
            }

            return value.toLowerCase( Locale.ENGLISH )
                        .equals( other.getValue()
                                      .toLowerCase( Locale.ENGLISH ) );
        }

        return zci.equals( ozci );
    }

}
