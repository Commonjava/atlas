package org.commonjava.maven.atlas.ident.jackson;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectVersionRefSerializerModuleTest
{

    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new ObjectMapper();
        mapper.registerModule( new ProjectVersionRefSerializerModule() );
    }

    @Test
    public void projectRefRoundTrip()
        throws Exception
    {
        final ProjectRef pr = new ProjectRef( "org.foo", "bar" );
        final String json = mapper.writeValueAsString( pr );

        final ProjectRef result = mapper.readValue( json, ProjectRef.class );

        assertThat( result, equalTo( pr ) );
    }

    @Test
    public void mapWithProjectRefKeyRoundTrip()
        throws Exception
    {
        final ProjectRef pr = new ProjectRef( "org.foo", "bar" );
        final String value = "this is the value";

        final Map<ProjectRef, String> map = new HashMap<ProjectRef, String>();
        map.put( pr, value );

        final String json = mapper.writeValueAsString( map );

        final Map<ProjectRef, String> result = mapper.readValue( json, new TypeReference<Map<ProjectRef, String>>()
        {
        } );

        assertThat( result.get( pr ), equalTo( value ) );
    }

}
