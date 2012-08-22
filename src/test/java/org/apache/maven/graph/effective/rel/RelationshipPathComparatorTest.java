package org.apache.maven.graph.effective.rel;

import static org.apache.maven.graph.effective.util.EGraphUtils.dependency;
import static org.apache.maven.graph.effective.util.EGraphUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.graph.common.ref.VersionedProjectRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.junit.Test;

public class RelationshipPathComparatorTest
{

    @Test
    public void sortParentDependencyPathAheadOfDirectDependency()
        throws InvalidVersionSpecificationException
    {
        final List<List<ProjectRelationship<?>>> paths = new ArrayList<List<ProjectRelationship<?>>>();

        List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();

        final VersionedProjectRef root = projectVersion( "group.id", "my-artifact", "1.0" );

        final VersionedProjectRef dep = projectVersion( "org.group", "dep-1", "1.0" );
        rels.add( dependency( root, dep, 0 ) );
        rels.add( dependency( dep, projectVersion( "org.foo", "bar", "1.0" ), 0 ) );

        paths.add( rels );

        rels = new ArrayList<ProjectRelationship<?>>();

        final VersionedProjectRef parent = projectVersion( "group.id", "parent", "1" );

        rels.add( new ParentRelationship( root, parent ) );
        rels.add( dependency( parent, "org.foo", "bar", "1.1.1", 0 ) );

        paths.add( rels );

        Collections.sort( paths, new RelationshipPathComparator() );

        final List<ProjectRelationship<?>> result = paths.get( 0 );
        final ProjectRelationship<?> firstResult = result.get( 0 );

        assertThat( ( firstResult instanceof ParentRelationship ), equalTo( true ) );
    }

}
