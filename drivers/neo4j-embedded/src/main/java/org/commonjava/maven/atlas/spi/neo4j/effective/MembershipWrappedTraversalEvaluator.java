package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.List;

import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

public class MembershipWrappedTraversalEvaluator
    implements Evaluator
{

    private final AbstractNeo4JEGraphDriver driver;

    private final ProjectNetTraversal traversal;

    private final int pass;

    public MembershipWrappedTraversalEvaluator( final AbstractNeo4JEGraphDriver driver,
                                                final ProjectNetTraversal traversal, final int pass )
    {
        this.driver = driver;
        this.traversal = traversal;
        this.pass = pass;
    }

    public Evaluation evaluate( final Path path )
    {
        final Relationship rel = path.lastRelationship();
        final Node node = path.endNode();

        if ( rel == null )
        {
            return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        if ( driver.inMembership( node ) && driver.inMembership( rel ) )
        {
            final List<ProjectRelationship<?>> relPath = driver.convertToRelationships( path.relationships() );

            if ( relPath.isEmpty() )
            {
                return Evaluation.EXCLUDE_AND_CONTINUE;
            }
            else
            {
                final ProjectRelationship<?> projectRel = relPath.remove( relPath.size() - 1 );
                if ( traversal.preCheck( projectRel, relPath, pass ) )
                {
                    return Evaluation.INCLUDE_AND_CONTINUE;
                }
            }
        }

        return Evaluation.EXCLUDE_AND_PRUNE;
    }

}
