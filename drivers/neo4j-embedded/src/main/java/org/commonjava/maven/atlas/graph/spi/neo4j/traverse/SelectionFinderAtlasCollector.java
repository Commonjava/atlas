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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.SelectionInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

public class SelectionFinderAtlasCollector
    implements AtlasCollector<SelectionInfo>
{

    private static final int SELECTED_IDX = 0;

    private static final int VARIABLE_IDX = 1;

    //    protected final Logger logger = new Logger( getClass() );

    protected Direction direction = Direction.OUTGOING;

    protected final Set<Node> startNodes;

    protected final Set<Long> seen = new HashSet<Long>();

    protected final ProjectRelationshipFilter filter;

    private final Map<Node, Relationship[]> raw = new HashMap<Node, Relationship[]>();

    private Set<SelectionInfo> infos;

    public SelectionFinderAtlasCollector( final Node start, final ProjectRelationshipFilter filter )
    {
        this( Collections.singleton( start ), filter );
    }

    public SelectionFinderAtlasCollector( final Set<Node> startNodes, final ProjectRelationshipFilter filter )
    {
        this.startNodes = startNodes;
        this.filter = filter;
    }

    private SelectionFinderAtlasCollector( final Set<Node> startNodes, final ProjectRelationshipFilter filter, final Direction direction )
    {
        this( startNodes, filter );
        this.direction = direction;
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public final Iterable<Relationship> expand( final Path path, final BranchState state )
    {
        if ( !startNodes.contains( path.startNode() ) )
        {
            //            logger.info( "Rejecting path; it does not start with one of our roots:\n\t%s", path );
            return Collections.emptySet();
        }

        final Long endId = path.endNode()
                               .getId();

        if ( seen.contains( endId ) )
        {
            //            logger.info( "Rejecting path; already seen it:\n\t%s", path );
            return Collections.emptySet();
        }

        seen.add( endId );

        if ( accept( path ) )
        {
            final Relationship r = path.lastRelationship();
            if ( r != null )
            {
                if ( Conversions.getBooleanProperty( Conversions.DESELECTED, r ) )
                {
                    final Node node = r.getStartNode();
                    Relationship[] raw = this.raw.get( node );
                    if ( raw == null )
                    {
                        raw = new Relationship[2];
                        this.raw.put( node, raw );
                    }

                    raw[VARIABLE_IDX] = r;

                    return Collections.emptySet();
                }
                else if ( Conversions.getBooleanProperty( Conversions.SELECTED, r ) )
                {
                    final Node node = r.getStartNode();
                    Relationship[] raw = this.raw.get( node );
                    if ( raw == null )
                    {
                        raw = new Relationship[2];
                        this.raw.put( node, raw );
                    }

                    raw[SELECTED_IDX] = r;

                    return Collections.emptySet();
                }
            }
        }

        return path.endNode()
                   .getRelationships( direction );
    }

    protected boolean accept( final Path path )
    {
        ProjectRelationshipFilter f = filter;
        for ( final Relationship r : path.relationships() )
        {
            //            logger.info( "Checking relationship for acceptance: %s", r );
            if ( f != null )
            {
                final ProjectRelationship<?> rel = Conversions.toProjectRelationship( r );
                if ( !f.accept( rel ) )
                {
                    //                    logger.info( "Filter rejected relationship: %s", rel );
                    return false;
                }

                f = f.getChildFilter( rel );
            }
        }

        //        logger.info( "Path accepted: %s", path );
        return true;
    }

    @Override
    public final Evaluation evaluate( final Path path )
    {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public PathExpander reverse()
    {
        return new SelectionFinderAtlasCollector( startNodes, filter, direction.reverse() );
    }

    public synchronized void clearSelectionInfos()
    {
        infos = null;
    }

    public synchronized Set<SelectionInfo> getSelectionInfos()
    {
        if ( infos == null )
        {
            infos = new HashSet<SelectionInfo>( raw.size() );
            for ( final Map.Entry<Node, Relationship[]> entry : raw.entrySet() )
            {
                final Relationship v = entry.getValue()[VARIABLE_IDX];
                final Relationship s = entry.getValue()[SELECTED_IDX];

                if ( v == null || s == null )
                {
                    /*@formatter:off*/
//                    logger.error( "\n" +
//                                  "\n" +
//                                  "\n" +
//                                  "\nERROR!" +
//                                  "\n" +
//                                  "\nFound one side of version-selection pair without the other:" +
//                                  "\n" +
//                                  "\nFrom node: %s" +
//                                  "\nVariable relationship: %s" +
//                                  "\nSelected relationship: %s\n" +
//                                  "\n" +
//                                  "\n" +
//                                  "\n",
//                                  entry.getKey(), v, s );
                    /*@formatter:on*/
                    continue;
                }

                infos.add( new SelectionInfo( v.getEndNode(), v, s.getEndNode(), s ) );
            }

            infos = Collections.unmodifiableSet( infos );
        }

        return infos;
    }

    @Override
    public Iterator<SelectionInfo> iterator()
    {
        return getSelectionInfos().iterator();
    }

}
