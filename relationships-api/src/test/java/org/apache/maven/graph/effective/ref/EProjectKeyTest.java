/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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
package org.apache.maven.graph.effective.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class EProjectKeyTest
{

    @Rule
    public TestName naming = new TestName();

    private URI testURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    @Test
    public void serializability()
        throws Exception
    {
        assertThat( Serializable.class.isAssignableFrom( EProjectKey.class ), equalTo( true ) );
        assertThat( Serializable.class.isAssignableFrom( ProjectVersionRef.class ), equalTo( true ) );

        final URI source = testURI();

        final EProjectKey key = new EProjectKey( source, new ProjectVersionRef( "org.foo", "bar", "1.0" ) );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oout = new ObjectOutputStream( baos );

        oout.writeObject( key );

        final ObjectInputStream oin = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
        final EProjectKey result = (EProjectKey) oin.readObject();

        assertThat( key.getSource(), equalTo( result.getSource() ) );
        assertThat( key.getProject(), equalTo( result.getProject() ) );
    }

}
