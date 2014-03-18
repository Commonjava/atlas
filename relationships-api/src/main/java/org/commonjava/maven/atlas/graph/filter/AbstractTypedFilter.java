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
package org.commonjava.maven.atlas.graph.filter;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public abstract class AbstractTypedFilter
    implements ProjectRelationshipFilter
{

    //    private final Logger logger = new Logger( getClass() );

    private final Set<RelationshipType> types;

    private final Set<RelationshipType> descendantTypes;

    private final boolean includeManagedInfo;

    private final boolean includeConcreteInfo;

    private transient String longId;

    private transient String shortId;

    protected AbstractTypedFilter( final RelationshipType type, final RelationshipType descendantType, final boolean includeManagedInfo,
                                   final boolean includeConcreteInfo )
    {
        this.types = Collections.unmodifiableSet( Collections.singleton( type ) );
        this.descendantTypes = Collections.unmodifiableSet( Collections.singleton( descendantType ) );
        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    protected AbstractTypedFilter( final RelationshipType type, final Collection<RelationshipType> descendantTypes, final boolean includeManagedInfo,
                                   final boolean includeConcreteInfo )
    {
        if ( type == null )
        {
            this.types = Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.types = Collections.unmodifiableSet( Collections.singleton( type ) );
        }

        if ( descendantTypes == null || descendantTypes.isEmpty() )
        {
            this.descendantTypes = Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.descendantTypes = Collections.unmodifiableSet( new HashSet<RelationshipType>( descendantTypes ) );
        }

        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    protected AbstractTypedFilter( final Collection<RelationshipType> types, final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        if ( types == null || types.isEmpty() )
        {
            this.types = Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.types = Collections.unmodifiableSet( new HashSet<RelationshipType>( types ) );
        }

        if ( descendantTypes == null || descendantTypes.isEmpty() )
        {
            this.descendantTypes = Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.descendantTypes = Collections.unmodifiableSet( new HashSet<RelationshipType>( descendantTypes ) );
        }

        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    protected AbstractTypedFilter( final RelationshipType type, final boolean hasDescendants, final boolean includeManagedInfo,
                                   final boolean includeConcreteInfo )
    {
        if ( type == null )
        {
            this.types = Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.types = Collections.unmodifiableSet( Collections.singleton( type ) );
        }

        if ( hasDescendants )
        {
            this.descendantTypes = types;
        }
        else
        {
            this.descendantTypes = Collections.unmodifiableSet( Collections.<RelationshipType> emptySet() );
        }

        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    public boolean isManagedInfoIncluded()
    {
        return includeManagedInfo;
    }

    public boolean isConcreteInfoIncluded()
    {
        return includeConcreteInfo;
    }

    @Override
    public final boolean accept( final ProjectRelationship<?> rel )
    {
        if ( types.contains( rel.getType() ) )
        {
            final boolean accepted = doAccept( rel );
            if ( accepted )
            {
                //                logger.info( "ACCEPT relationship: %s. Type is in: %s, and was accepted in second-level analysis", rel,
                //                             types );
            }
            else
            {
                //                logger.info( "REJECT relationship: %s. Type is in: %s but was rejected by second-level analysis.", rel,
                //                             types );
            }

            return accepted;
        }
        //        else
        //        {
        //            logger.info( "REJECT relationship: %s. Type is not in: %s", rel, types );
        //        }

        return false;
    }

    public Set<RelationshipType> getRelationshipTypes()
    {
        return types;
    }

    public Set<RelationshipType> getDescendantRelationshipTypes()
    {
        return descendantTypes;
    }

    protected boolean doAccept( final ProjectRelationship<?> rel )
    {
        // base functionality is only to check that the type is appropriate.
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( descendantTypes == null ) ? 0 : descendantTypes.hashCode() );
        result = prime * result + ( includeConcreteInfo ? 1231 : 1237 );
        result = prime * result + ( includeManagedInfo ? 1231 : 1237 );
        result = prime * result + ( ( types == null ) ? 0 : types.hashCode() );
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
        final AbstractTypedFilter other = (AbstractTypedFilter) obj;
        if ( descendantTypes == null )
        {
            if ( other.descendantTypes != null )
            {
                return false;
            }
        }
        else if ( !descendantTypes.equals( other.descendantTypes ) )
        {
            return false;
        }
        if ( includeConcreteInfo != other.includeConcreteInfo )
        {
            return false;
        }
        if ( includeManagedInfo != other.includeManagedInfo )
        {
            return false;
        }
        if ( types == null )
        {
            if ( other.types != null )
            {
                return false;
            }
        }
        else if ( !types.equals( other.types ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            final String abbreviatedPackage = getClass().getPackage()
                                                        .getName()
                                                        .replaceAll( "([a-zA-Z])[a-zA-Z]+", "$1" );

            sb.append( abbreviatedPackage )
              .append( '.' )
              .append( getClass().getSimpleName() )
              .append( "[types:{" )
              .append( join( types, "," ) )
              .append( "},next-types:{" )
              .append( join( descendantTypes, "," ) )
              .append( ",concrete:" )
              .append( includeConcreteInfo )
              .append( ",managed:" )
              .append( includeManagedInfo );

            renderIdAttributes( sb );
            sb.append( ']' );

            longId = sb.toString();
        }

        return longId;
    }

    protected void renderIdAttributes( final StringBuilder sb )
    {
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public String getCondensedId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }
}
