/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.junit.Test;

public class EProjectKeyTest
{

    @Test
    public void serializability()
        throws Exception
    {
        assertThat( Serializable.class.isAssignableFrom( EProjectKey.class ), equalTo( true ) );
        assertThat( Serializable.class.isAssignableFrom( EGraphFacts.class ), equalTo( true ) );
        assertThat( Serializable.class.isAssignableFrom( ProjectVersionRef.class ), equalTo( true ) );

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
