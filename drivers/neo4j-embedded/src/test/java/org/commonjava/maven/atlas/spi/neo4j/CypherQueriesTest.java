package org.commonjava.maven.atlas.spi.neo4j;

import java.util.Map;

import org.apache.log4j.Level;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionUtils;
import org.apache.maven.graph.effective.EProjectGraph;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.spi.neo4j.effective.AbstractNeo4JEGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.fixture.MemoryDriverFixture;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionResult;

public class CypherQueriesTest
{

    @Rule
    public MemoryDriverFixture fixture = new MemoryDriverFixture();

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Test
    public void projectsWithVariableFlag_PartialQuery()
        throws Exception
    {
        final ProjectVersionRef project = new ProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new ProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new ProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final SingleVersion selected = VersionUtils.createSingleVersion( "1.0-20130314.161200-1" );

        /* @formatter:off */
        final EProjectGraph graph =
            new EProjectGraph.Builder( new EProjectKey( project ), fixture.newDriverInstance() )
                .withDependencies( 
                    new DependencyRelationship( project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                    new DependencyRelationship( varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false )
                )
            .build();

        graph.selectVersionFor( varDep, selected );
        
        final String cypher = "START a=node(1) "
            + "\nMATCH p=(a)-[:M_PLUGIN_DEP|C_PLUGIN|PARENT|EXTENSION|M_DEPENDENCY|M_PLUGIN|C_PLUGIN_DEP|C_DEPENDENCY*]->(n) "
            + "\nWHERE "
            + "\n  none( "
            + "\n    r in relationships(p) "
            + "\n      WHERE has(r._deselected_for) "
            + "\n  ) "
            + "\nRETURN n as node, p as path";
        /* @formatter:on */

        final AbstractNeo4JEGraphDriver driver = (AbstractNeo4JEGraphDriver) graph.getDriver();

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
        final SingleVersion selected = VersionUtils.createSingleVersion( "1.0-20130314.161200-1" );

        /* @formatter:off */
        final EProjectGraph graph =
            new EProjectGraph.Builder( new EProjectKey( project ), fixture.newDriverInstance() )
                .withDependencies( 
                    new DependencyRelationship( project, new ArtifactRef( varDep, null, null, false ), null, 0, false ),
                    new DependencyRelationship( varDep,  new ArtifactRef( varD2,  null, null, false ), null, 0, false )
                )
            .build();

        graph.selectVersionFor( varDep, selected );
        
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

        final AbstractNeo4JEGraphDriver driver = (AbstractNeo4JEGraphDriver) graph.getDriver();

        final ExecutionResult result = driver.execute( cypher );
        int i = 0;
        for ( final Map<String, Object> record : result )
        {
            System.out.printf( "%d:  %s", i++, record );
        }
    }

}
