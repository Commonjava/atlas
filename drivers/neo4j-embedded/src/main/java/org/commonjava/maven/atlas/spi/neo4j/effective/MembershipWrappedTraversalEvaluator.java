package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

public class MembershipWrappedTraversalEvaluator
    implements Evaluator
{

    private final Logger logger = new Logger( getClass() );

    private final AbstractNeo4JEGraphDriver driver;

    private final ProjectNetTraversal traversal;

    private final Set<Long> seenRels = new HashSet<Long>();

    private int check = 0;

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
        check++;

        final Relationship rel = path.lastRelationship();
        if ( rel == null )
        {
            logger.info( "[%d] Relationship is null. Continue traversal.", check );
            return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        if ( seenRels.contains( rel.getId() ) )
        {
            logger.info( "[%d] Already saw relationship: %d. Skipping.", check, rel.getId() );
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        seenRels.add( rel.getId() );

        final Node node = path.endNode();

        if ( driver.inMembership( node ) && driver.inMembership( rel ) )
        {
            final ProjectRelationship<?> lastRel = Conversions.toProjectRelationship( rel );
            logger.info( "[%d] Rel: %s is in membership; checking vs filter: %s", check, lastRel, traversal );

            if ( lastRel.getDeclaring()
                        .equals( lastRel.getTarget() ) && lastRel.getType() == RelationshipType.PARENT )
            {
                logger.info( "[%d] Detected terminal parent: %s", check, lastRel );
                //                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            final List<ProjectRelationship<?>> relPath = driver.convertToRelationships( path.relationships() );
            logger.info( "\n\n\n\n[%d] Path:\n  %s\n\n\n", check, StringUtils.join( relPath, "\n  " ) );

            if ( relPath.isEmpty() )
            {
                logger.info( "[%d] Relationship path is empty. Skipping this relationship, but continuing to traverse beyond it.",
                             check );
                return Evaluation.EXCLUDE_AND_CONTINUE;
            }
            else
            {
                logger.info( "[%d] Relationship path has %d elements.", check, relPath.size() );
                final ProjectRelationship<?> projectRel = relPath.remove( relPath.size() - 1 );
                logger.info( "[%d] Checking relationship: %s vs filter: %s.", check, projectRel, traversal );
                if ( traversal.preCheck( projectRel, relPath, pass ) )
                {
                    logger.info( "[%d] Included by filter.", check );
                    return Evaluation.INCLUDE_AND_CONTINUE;
                }
            }
        }

        logger.info( "[%d] Exclude and prune.", check );
        return Evaluation.EXCLUDE_AND_PRUNE;
    }

}
