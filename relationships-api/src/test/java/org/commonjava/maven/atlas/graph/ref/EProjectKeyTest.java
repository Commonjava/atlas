/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
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
