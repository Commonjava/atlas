package org.apache.maven.graph.effective.traverse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.spi.GraphDriverException;

public class PrintingTraversal
    extends AbstractTraversal
{

    private final StringWriter buffer = new StringWriter();

    private final PrintWriter writer = new PrintWriter( buffer );

    private final String indent;

    private final ProjectNetTraversal traversal;

    private final String header;

    private final String footer;

    public PrintingTraversal()
    {
        this( null, null, null, "  ", 1, TraversalType.depth_first );
    }

    public PrintingTraversal( final ProjectNetTraversal traversal )
    {
        this( traversal, null, null, "  ", 1, TraversalType.depth_first );
    }

    public PrintingTraversal( final String indent )
    {
        this( null, null, null, indent, 1, TraversalType.depth_first );
    }

    public PrintingTraversal( final ProjectNetTraversal traversal, final String indent )
    {
        this( traversal, null, null, indent, 1, TraversalType.depth_first );
    }

    public PrintingTraversal( final ProjectNetTraversal traversal, final String header, final String footer,
                              final String indent, final int passes, final TraversalType... types )
    {
        super( passes, types );
        this.traversal = traversal;
        this.header = header;
        this.footer = footer;
        this.indent = indent;
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        for ( int i = 0; i < path.size(); i++ )
        {
            writer.print( indent );
        }

        // TODO: Format this appropriately.
        writer.printf( "%s\n", relationship.getTarget() );

        return true;
    }

    public String getString()
    {
        return buffer.toString();
    }

    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                             final int pass )
    {
        return traversal == null || traversal.preCheck( relationship, path, pass );
    }

    @Override
    public void startTraverse( final int pass, final EProjectNet network )
        throws GraphDriverException
    {
        if ( traversal != null )
        {
            traversal.startTraverse( pass, network );
        }

        if ( pass == 0 && header != null )
        {
            writer.println( header );
        }
    }

    @Override
    public void endTraverse( final int pass, final EProjectNet network )
        throws GraphDriverException
    {
        if ( traversal != null )
        {
            traversal.endTraverse( pass, network );
        }

        if ( pass == getRequiredPasses() - 1 )
        {
            if ( footer != null )
            {
                writer.println( footer );
            }
        }
        else
        {
            writer.println();
        }
    }

    @Override
    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                               final int pass )
    {
        if ( traversal != null )
        {
            traversal.edgeTraversed( relationship, path, pass );
        }
    }

}
