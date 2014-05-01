package org.commonjava.maven.atlas.graph.filter;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public final class StructuralRelationshipsFilter
    extends AbstractTypedFilter
{

    private static final Collection<RelationshipType> TYPES =
        Collections.unmodifiableCollection( Arrays.asList( PARENT, BOM ) );

    private static final long serialVersionUID = 1L;

    public static final StructuralRelationshipsFilter INSTANCE = new StructuralRelationshipsFilter();

    private StructuralRelationshipsFilter()
    {
        super( TYPES, TYPES, false, true );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

}
