/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.fixture.FileDriverFixture;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.neo4j.cypher.javacompat.ExecutionResult;

public class CypherQueriesTest
{

    @Rule
    public FileDriverFixture fixture = new FileDriverFixture();

    @Rule
    public TestName naming = new TestName();

    private URI sourceURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    protected synchronized EGraphManager getManager()
        throws Exception
    {
        return fixture.manager();
    }

    @Test
    public void projectsWithVariableFlag_PartialQuery()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final GraphWorkspace workspace = getManager().createWorkspace( new GraphWorkspaceConfiguration().withSource( source ) );

        /* @formatter:off */
        getManager().storeRelationships( workspace,
            new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
            new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false )
        );
        
        final EProjectGraph graph = getManager().getGraph( workspace, project );

        workspace.selectVersion( varDep, selected );
        
        final String cypher = "START a=node(1) "
            + "\nMATCH p=(a)-[:M_PLUGIN_DEP|C_PLUGIN|PARENT|EXTENSION|M_DEPENDENCY|M_PLUGIN|C_PLUGIN_DEP|C_DEPENDENCY*]->(n) "
            + "\nWHERE "
            + "\n  none( "
            + "\n    r in relationships(p) "
            + "\n      WHERE has(r._deselected_for) "
            + "\n  ) "
            + "\nRETURN n as node, p as path";
        /* @formatter:on */

        final AbstractNeo4JEGraphDriver driver = (AbstractNeo4JEGraphDriver) graph.getDatabase();

        final ExecutionResult result = driver.execute( cypher );
        int i = 0;
        for ( final Map<String, Object> record : result )
        {
            System.out.printf( "%d:  %s", i++, record );
        }
    }

    @Test
    //    @Ignore
    public void projectsWithVariableFlagQuery()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new ProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final GraphWorkspace workspace = getManager().createWorkspace( new GraphWorkspaceConfiguration().withSource( source ) );

        /* @formatter:off */
        getManager().storeRelationships( workspace,
            new DependencyRelationship( source, project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
            new DependencyRelationship( source, varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false )
        );
        
        final EProjectGraph graph = getManager().getGraph( workspace, project );

        workspace.selectVersion( varDep, selected );
        
        final String cypher = "START a=node(1) "
            + "\nMATCH p=(a)-[:M_PLUGIN_DEP|C_PLUGIN|PARENT|EXTENSION|M_DEPENDENCY|M_PLUGIN|C_PLUGIN_DEP|C_DEPENDENCY*]->(n) "
            + "\nWHERE "
            + "\n  none( "
            + "\n    r in relationships(p) "
            + "\n      WHERE has(r._deselected_for) "
            + "\n  ) "
            + "\n  AND has(n.gav) "
            + "\n  AND has(n.gav) "
            + "\n  AND n._variable! = true "
            + "\nRETURN n as node, p as path";
        /* @formatter:on */

        final AbstractNeo4JEGraphDriver driver = (AbstractNeo4JEGraphDriver) graph.getDatabase();

        final ExecutionResult result = driver.execute( cypher );
        int i = 0;
        for ( final Map<String, Object> record : result )
        {
            System.out.printf( "%d:  %s", i++, record );
        }
    }

}
