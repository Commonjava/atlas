package org.commonjava.maven.atlas.graph.filter;

import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class RelationshipTypeFilter
    extends AbstractTypedFilter
{

    private static final long serialVersionUID = 1L;

    public RelationshipTypeFilter( final Collection<RelationshipType> types, final boolean includeManagedInfo,
                                   final boolean includeConcreteInfo )
    {
        super( types, types, includeManagedInfo, includeManagedInfo );
    }

    public RelationshipTypeFilter( final Collection<RelationshipType> types,
                                   final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( types, descendantTypes, includeManagedInfo, includeConcreteInfo );
    }

    public RelationshipTypeFilter( final RelationshipType type, final boolean hasDescendants,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( type, hasDescendants, includeManagedInfo, includeConcreteInfo );
    }

    public RelationshipTypeFilter( final RelationshipType type, final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( type, descendantTypes, includeManagedInfo, includeConcreteInfo );
    }

    public RelationshipTypeFilter( final RelationshipType type, final RelationshipType descendantType,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( type, descendantType, includeManagedInfo, includeConcreteInfo );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        final Set<RelationshipType> descendantTypes = getDescendantRelationshipTypes();
        final Set<RelationshipType> allowedTypes = getAllowedTypes();
        if ( !allowedTypes.equals( descendantTypes ) )
        {
            return new RelationshipTypeFilter( descendantTypes, includeManagedRelationships(),
                                               includeConcreteRelationships() );
        }

        return this;
    }

}
