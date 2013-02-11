package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

public class MembershipWrappedTraversalEvaluator<STATE>
    implements Evaluator, PathExpander<STATE>
{

    //    private final Logger logger = new Logger( getClass() );

    private final AbstractNeo4JEGraphDriver driver;

    private final ProjectNetTraversal traversal;

    private final Set<Long> seenRels = new HashSet<Long>();

    private final int pass;

    private boolean reversedExpander;

    public MembershipWrappedTraversalEvaluator( final AbstractNeo4JEGraphDriver driver,
                                                final ProjectNetTraversal traversal, final int pass )
    {
        this.driver = driver;
        this.traversal = traversal;
        this.pass = pass;
    }

    private MembershipWrappedTraversalEvaluator( final MembershipWrappedTraversalEvaluator<STATE> ev,
                                                 final boolean reversedExpander )
    {
        this.driver = ev.driver;
        this.traversal = ev.traversal;
        this.pass = ev.pass;
        this.reversedExpander = reversedExpander;
    }

    public Evaluation evaluate( final Path path )
    {
        //        check++;

        final Relationship rel = path.lastRelationship();
        if ( rel == null )
        {
            //            logger.info( "[%d] Relationship is null. Continue traversal.", check );
            return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        if ( seenRels.contains( rel.getId() ) )
        {
            //            logger.info( "[%d] Already saw relationship: %d. Skipping.", check, rel.getId() );
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        //        logger.info( "Adding: %s to seen relationships: %s", rel, seenRels );
        seenRels.add( rel.getId() );

        final Node node = path.endNode();

        if ( driver.inMembership( node ) && driver.inMembership( rel ) )
        {
            final ProjectRelationship<?> lastRel = Conversions.toProjectRelationship( rel );
            //            logger.info( "[%d] Rel: %s is in membership; checking vs filter: %s", check, lastRel, traversal );

            if ( lastRel.getDeclaring()
                        .equals( lastRel.getTarget() ) && lastRel.getType() == RelationshipType.PARENT )
            {
                //                logger.info( "[%d] Detected terminal parent: %s", check, lastRel );
                //                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            final List<ProjectRelationship<?>> relPath = driver.convertToRelationships( path.relationships() );
            //            logger.info( "\n\n\n\n[%d] Path:\n  %s\n\n\n", check, StringUtils.join( relPath, "\n  " ) );

            if ( relPath.isEmpty() )
            {
                //                logger.info( "[%d] Relationship path is empty. Skipping this relationship, but continuing to traverse beyond it.",
                //                             check );
                return Evaluation.EXCLUDE_AND_CONTINUE;
            }
            else
            {
                //                logger.info( "[%d] Relationship path has %d elements.", check, relPath.size() );
                final ProjectRelationship<?> projectRel = relPath.remove( relPath.size() - 1 );
                //                logger.info( "[%d] Checking relationship: %s vs filter: %s.", check, projectRel, traversal );
                if ( traversal.preCheck( projectRel, relPath, pass ) )
                {
                    //                    logger.info( "[%d] Included by filter.", check );
                    return Evaluation.INCLUDE_AND_CONTINUE;
                }
            }
        }

        //        logger.info( "[%d] Exclude and prune.", check );
        return Evaluation.EXCLUDE_AND_PRUNE;
    }

    public Iterable<Relationship> expand( final Path path, final BranchState<STATE> state )
    {
        final Node node = path.endNode();
        //        logger.info( "START expansion for node: %s", node );

        // TODO: Is node(0) appropriate to see??
        if ( node.getId() != 0 && !driver.inMembership( node ) )
        {
            //            logger.info( "%s not in membership. Skipping expansion.", node );
            return Collections.emptySet();
        }

        List<ProjectRelationship<?>> rels;
        Iterable<Relationship> rs = path.relationships();
        if ( rs == null )
        {
            //            logger.info( "Constructing empty relationship list for filter." );
            rels = Collections.emptyList();
        }
        else
        {
            rels = driver.convertToRelationships( rs );
            //            logger.info( "Got relationship list %d entries long for filter", rels.size() );
        }

        if ( reversedExpander )
        {
            //            logger.info( "Reversing relationship list for filter." );
            Collections.reverse( rels );
        }

        final Relationship rel = path.lastRelationship();
        if ( rel != null )
        {
            if ( !driver.inMembership( rel ) )
            {
                //                logger.info( "Last relationship (%s) is not in membership.", rel );
                return Collections.emptySet();
            }
        }

        rs = node.getRelationships( reversedExpander ? Direction.INCOMING : Direction.OUTGOING );
        if ( rs == null )
        {
            //            logger.info( "No relationships from end-node: %s", node );
            return Collections.emptySet();
        }

        final Set<Relationship> result = new HashSet<Relationship>();
        for ( final Relationship r : rs )
        {
            //            logger.info( "Pre-checking relationship %s for expansion using filter: %s", r, traversal );
            final ProjectRelationship<?> projectRel = Conversions.toProjectRelationship( r );
            if ( traversal.preCheck( projectRel, rels, pass ) )
            {
                //                logger.info( "Adding for expansion: %s", r );
                result.add( r );
            }
        }

        //        logger.info( "Expanding for %d relationships.", result.size() );
        return result;
    }

    public PathExpander<STATE> reverse()
    {
        return new MembershipWrappedTraversalEvaluator<STATE>( this, true );
    }

}
