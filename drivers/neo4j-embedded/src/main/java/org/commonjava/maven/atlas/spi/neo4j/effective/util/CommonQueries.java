package org.commonjava.maven.atlas.spi.neo4j.effective.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.spi.neo4j.effective.AbstractNeo4JEGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.util.logging.Logger;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Path;

public class CommonQueries
{

    private static final String ANCESTRY = " MATCH p=(n)-[:PARENT*]->() RETURN p as ancestry";

    //    private static final String ANCESTRY = " MATCH p=(n)-->() RETURN p as ancestry";

    public static List<ProjectVersionRef> ancestryOf( final ProjectVersionRef ref,
                                                      final AbstractNeo4JEGraphDriver driver )
        throws GraphDriverException
    {
        final Logger logger = new Logger( CommonQueries.class );

        final ExecutionResult result = driver.executeFrom( ANCESTRY, ref );
        final Iterator<Path> pathIt = result.columnAs( "ancestry" );

        int longest = 0;
        Path longestPath = null;
        while ( pathIt.hasNext() )
        {
            final Path p = pathIt.next();
            logger.info( "Found path: %s", p );

            if ( longestPath == null || p.length() > longest )
            {
                longestPath = p;
                longest = p.length();
            }
        }

        return longestPath == null ? null : toNodes( longestPath, driver );
    }

    //    private static List<ProjectVersionRef> prepend( final ProjectVersionRef ref, final List<ProjectVersionRef> nodes )
    //    {
    //        if ( nodes == null )
    //        {
    //            return null;
    //        }
    //
    //        final List<ProjectVersionRef> result = new ArrayList<ProjectVersionRef>( nodes );
    //        result.add( 0, ref );
    //
    //        return result;
    //    }

    private static List<ProjectVersionRef> toNodes( final Path p, final AbstractNeo4JEGraphDriver driver )
    {
        final List<ProjectVersionRef> result =
            new ArrayList<ProjectVersionRef>( Conversions.convertToProjects( p.nodes() ) );

        final int len = result.size();
        if ( len > 1 && result.get( len - 1 )
                              .equals( result.get( len - 2 ) ) )
        {
            result.remove( len - 1 );
        }

        return result;
    }

}
