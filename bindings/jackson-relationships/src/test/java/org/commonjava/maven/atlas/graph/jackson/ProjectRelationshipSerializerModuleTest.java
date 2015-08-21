package org.commonjava.maven.atlas.graph.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 8/21/15.
 */
public class ProjectRelationshipSerializerModuleTest
{

    private ObjectMapper mapper;

    @Before
    public void before()
    {
        mapper = new ObjectMapper();
        mapper.registerModules( new ProjectVersionRefSerializerModule(), new ProjectRelationshipSerializerModule() );
    }

    @Test
    public void roundTrip_TerminalParentRelationship()
            throws Exception
    {
        ParentRelationship rel = new ParentRelationship( new ProjectVersionRef( "org.foo", "bar", "1" ) );

        String json = mapper.writeValueAsString( rel );
        System.out.println( json );

        ProjectRelationship<?> result = mapper.readValue( json, ProjectRelationship.class );

        assertThat( (ParentRelationship) result, equalTo( rel ) );
    }

    @Test
    public void roundTrip_ParentRelationship()
            throws Exception
    {
        ParentRelationship rel = new ParentRelationship( URI.create( "some:test:location" ),
                                                         new ProjectVersionRef( "org.foo", "bar", "1" ),
                                                         new ProjectVersionRef( "org.foo", "parent", "1001" ) );

        String json = mapper.writeValueAsString( rel );
        System.out.println( json );

        ProjectRelationship<?> result = mapper.readValue( json, ProjectRelationship.class );

        assertThat( (ParentRelationship) result, equalTo( rel ) );
    }

    @Test
    public void roundTrip_SimpleConcreteDependency()
            throws Exception
    {
        DependencyRelationship rel =
                new DependencyRelationship( URI.create( "some:test:location" ), RelationshipUtils.POM_ROOT_URI,
                                            new ProjectVersionRef( "org.foo", "bar", "1" ),
                                            new ProjectVersionRef( "org.foo", "dep", "1.1" ).asJarArtifact(),
                                            DependencyScope.compile, 0, false );

        String json = mapper.writeValueAsString( rel );
        System.out.println( json );

        ProjectRelationship<?> result = mapper.readValue( json, ProjectRelationship.class );

        assertThat( (DependencyRelationship) result, equalTo( rel ) );
    }
}
