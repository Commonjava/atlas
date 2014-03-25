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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipWrappedTraversalEvaluator<STATE>
    implements Evaluator, PathExpander<STATE>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<Long> rootIds;

    private final ProjectNetTraversal traversal;

    private final Map<Neo4jGraphPath, GraphPathInfo> pathInfos = new HashMap<Neo4jGraphPath, GraphPathInfo>();

    private final int pass;

    private boolean reversedExpander;

    private final Set<String> seenKeys = new HashSet<String>();

    private final GraphView view;

    private ConversionCache cache = new ConversionCache();

    private final GraphAdmin admin;

    public MembershipWrappedTraversalEvaluator( final Set<Long> rootIds, final ProjectNetTraversal traversal, final GraphView view,
                                                final GraphAdmin admin, final int pass )
    {
        this.rootIds = rootIds;
        this.traversal = traversal;
        this.view = view;
        this.admin = admin;
        this.pass = pass;
    }

    private MembershipWrappedTraversalEvaluator( final MembershipWrappedTraversalEvaluator<STATE> ev, final boolean reversedExpander )
    {
        this.rootIds = ev.rootIds;
        this.traversal = ev.traversal;
        this.pass = ev.pass;
        this.view = ev.view;
        this.admin = ev.admin;
        this.reversedExpander = reversedExpander;
    }

    public void setConversionCache( final ConversionCache cache )
    {
        this.cache = cache;
    }

    @Override
    public Evaluation evaluate( final Path path )
    {
        final Relationship rel = path.lastRelationship();
        if ( rel == null )
        {
            //            logger.info( "MISSING last-relationship: {}. exclude and continue", rel );
            return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        final Set<Long> roots = rootIds;
        if ( roots == null || roots.isEmpty() || roots.contains( path.startNode()
                                                                     .getId() ) )
        {
            final ProjectRelationship<?> lastRel = Conversions.toProjectRelationship( rel, cache );

            final List<ProjectRelationship<?>> relPath = Conversions.convertToRelationships( path.relationships(), cache );
            if ( relPath.indexOf( lastRel ) == relPath.size() - 1 )
            {
                //                logger.warn( "\n\n\n\n\nREMOVING last-relationship: {} from path!\n\n\n\n\n" );
                relPath.remove( relPath.size() - 1 );
            }

            if ( traversal.preCheck( lastRel, relPath, pass ) )
            {
                logger.debug( "Include-and-continue: {}, {}", relPath, lastRel );
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            else
            {
                logger.debug( "exclude-and-prune: {}, {}", relPath, lastRel );
            }
        }

        //        logger.info( "exclude and prune" );
        return Evaluation.EXCLUDE_AND_PRUNE;
    }

    @Override
    public Iterable<Relationship> expand( final Path path, final BranchState<STATE> state )
    {
        final Node node = path.endNode();
        //        logger.info( "START expansion for: {}", path );

        // TODO: Is node(0) appropriate to see??
        final Set<Long> roots = rootIds;
        if ( node.getId() != 0 && roots != null && roots.isEmpty() && !roots.contains( path.startNode()
                                                                                           .getId() ) )
        {
            //            logger.info( "{} not in membership. Skipping expansion.", node );
            return Collections.emptySet();
        }

        final Neo4jGraphPath graphPath = new Neo4jGraphPath( path );
        GraphPathInfo pathInfo = pathInfos.remove( graphPath );

        if ( pathInfo == null )
        {
            if ( path.lastRelationship() == null )
            {
                // just starting out. Initialize the path info.
                pathInfo = new GraphPathInfo( view );
            }
            else
            {
                return Collections.emptySet();
            }
        }

        final String key = graphPath.getKey() + "#" + pathInfo.getKey();
        if ( !seenKeys.add( key ) )
        {
            return Collections.emptySet();
        }

        //        final Relationship rel = path.lastRelationship();
        //        if ( rel != null )
        //        {
        //            final AbstractNeo4JEGraphDriver db = (AbstractNeo4JEGraphDriver) view.getDatabase();
        //            final Relationship sel = db == null ? null : db.select( rel, view );
        //
        //            if ( ( sel == null || sel == rel ) && Conversions.getBooleanProperty( Conversions.SELECTION, rel, false ) )
        //            {
        //                expMemberMisses++;
        //                return Collections.emptySet();
        //            }
        //        }
        //

        final Iterable<Relationship> rs = node.getRelationships( reversedExpander ? Direction.INCOMING : Direction.OUTGOING );
        if ( rs == null )
        {
            //            logger.info( "No relationships from end-node: {}", node );
            return Collections.emptySet();
        }

        final Set<Relationship> result = new HashSet<Relationship>();
        final List<ProjectRelationship<?>> rels = getPathRelationships( path );

        //        logger.info( "For: {} Determining which of {} child relationships to expand traversal into for: {}\n{}", traversal.getClass()
        //                                                                                                                          .getName(), path.length(),
        //                     path.endNode()
        //                         .hasProperty( GAV ) ? path.endNode()
        //                                                   .getProperty( GAV ) : "Unknown", new JoinString( "\n  ", Thread.currentThread()
        //                                                                                                                  .getStackTrace() ) );

        for ( Relationship r : rs )
        {
            if ( Conversions.getBooleanProperty( Conversions.CYCLES_INJECTED, r, false ) )
            {
                continue;
            }

            final Relationship selected = admin.select( r, view, pathInfo, graphPath );

            // if no selection happened and r is a selection-only relationship, skip it.
            if ( ( selected == null || selected == r ) && admin.isSelection( r, view ) )
            {
                continue;
            }

            if ( selected != null )
            {
                r = selected;
            }
            //            logger.info( "Attempting to expand: {}", r );

            final ProjectRelationship<?> projectRel = Conversions.toProjectRelationship( r, cache );
            final GraphPathInfo next = pathInfo.getChildPathInfo( projectRel );

            logger.debug( "Pre-checking relationship {} for expansion using filter: {}", projectRel, traversal );
            if ( traversal.preCheck( projectRel, rels, pass ) )
            {
                logger.debug( "Adding for expansion: {}", projectRel );
                pathInfos.put( new Neo4jGraphPath( graphPath, r.getId() ), next );
                result.add( r );
            }
            else
            {
                logger.debug( "Skipping for expansion: {}", projectRel );
            }
        }

        logger.debug( "Expanding for {} relationships.", result.size() );
        return result;
    }

    private List<ProjectRelationship<?>> getPathRelationships( final Path path )
    {
        List<ProjectRelationship<?>> rels;
        final Iterable<Relationship> rs = path.relationships();
        if ( rs == null )
        {
            //            logger.info( "Constructing empty relationship list for filter." );
            rels = Collections.emptyList();
        }
        else
        {
            rels = Conversions.convertToRelationships( rs, cache );
            //            logger.info( "Got relationship list {} entries long for filter", rels.size() );
        }

        if ( reversedExpander )
        {
            //            logger.info( "Reversing relationship list for filter." );
            Collections.reverse( rels );
        }

        return rels;
    }

    @Override
    public PathExpander<STATE> reverse()
    {
        return new MembershipWrappedTraversalEvaluator<STATE>( this, true );
    }

}
