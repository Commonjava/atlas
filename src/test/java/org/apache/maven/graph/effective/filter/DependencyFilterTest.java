package org.apache.maven.graph.effective.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.junit.Test;

public class DependencyFilterTest
{

    @Test
    public void rejectTestScopeForRuntimeFilter()
        throws Exception
    {
        final DependencyFilter filter = new DependencyFilter( DependencyScope.runtime );
        final DependencyRelationship rel =
            new DependencyRelationship( new ProjectVersionRef( "g", "a", "1" ), new ArtifactRef( "g", "b", "2", "jar",
                                                                                                 null, false ),
                                        DependencyScope.test, 0, false );

        assertThat( filter.accept( rel ), equalTo( false ) );
    }

}
