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
package org.commonjava.maven.atlas.spi.neo4j.traverse;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.effective.MemoryNeo4JEGraphDriver;
import org.commonjava.maven.atlas.tck.effective.traverse.AncestryTraversalTCK;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.After;
import org.junit.BeforeClass;

public class MemoryAncestryTraversalTest
    extends AncestryTraversalTCK
{
    private MemoryNeo4JEGraphDriver driver;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @After
    public void teardown()
        throws IOException
    {
        driver.close();
    }

    @Override
    protected EGraphDriver newDriverInstance()
        throws Exception
    {
        driver = new MemoryNeo4JEGraphDriver( false );
        return driver;
    }
}
