package org.apache.maven.graph.effective.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public abstract class AbstractTypedFilter
    implements ProjectRelationshipFilter
{

    private final Set<RelationshipType> types;

    private final Set<RelationshipType> descendantTypes;

    private final boolean includeManagedInfo;

    private final boolean includeConcreteInfo;

    protected AbstractTypedFilter( final RelationshipType type, final RelationshipType descendantType,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        this.types = Collections.unmodifiableSet( Collections.singleton( type ) );
        this.descendantTypes = Collections.unmodifiableSet( Collections.singleton( descendantType ) );
        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    protected AbstractTypedFilter( final RelationshipType type, final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        if ( type == null )
        {
            this.types =
                Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.types = Collections.unmodifiableSet( Collections.singleton( type ) );
        }

        if ( descendantTypes == null || descendantTypes.isEmpty() )
        {
            this.descendantTypes =
                Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.descendantTypes = Collections.unmodifiableSet( new HashSet<RelationshipType>( descendantTypes ) );
        }

        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    protected AbstractTypedFilter( final Collection<RelationshipType> types,
                                   final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        if ( types == null || types.isEmpty() )
        {
            this.types =
                Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.types = Collections.unmodifiableSet( new HashSet<RelationshipType>( types ) );
        }

        if ( descendantTypes == null || descendantTypes.isEmpty() )
        {
            this.descendantTypes =
                Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
        }
        else
        {
            this.descendantTypes = Collections.unmodifiableSet( new HashSet<RelationshipType>( descendantTypes ) );
        }

        this.includeManagedInfo = includeManagedInfo;
        this.includeConcreteInfo = includeConcreteInfo;
    }

    protected AbstractTypedFilter( final RelationshipType type, final boolean hasDescendants,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        if ( type == null )
        {
            this.types =
                Collections.unmodifiableSet( new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) ) );
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

    public final boolean accept( final ProjectRelationship<?> rel )
    {
        if ( types.contains( rel.getType() ) )
        {
            return doAccept( rel );
        }

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

}
