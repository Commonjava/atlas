package org.apache.maven.graph.effective.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.junit.Test;

public class EProjectKeyTest
{

    @Test
    public void serializability()
        throws Exception
    {
        final EProjectKey key =
            new EProjectKey( new ProjectVersionRef( "org.foo", "bar", "1.0" ), new EGraphFacts( "profile1" ) );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oout = new ObjectOutputStream( baos );

        oout.writeObject( key );

        final ObjectInputStream oin = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
        final EProjectKey result = (EProjectKey) oin.readObject();

        assertThat( key.getProject(), equalTo( result.getProject() ) );
        assertThat( key.getFacts(), equalTo( result.getFacts() ) );
    }

}
