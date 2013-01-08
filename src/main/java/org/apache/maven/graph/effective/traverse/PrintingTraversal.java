package org.apache.maven.graph.effective.traverse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class PrintingTraversal
    extends AbstractTraversal
{

    private final StringWriter buffer = new StringWriter();

    private final PrintWriter writer = new PrintWriter( buffer );

    private final String indent;

    public PrintingTraversal()
    {
        this( "  " );
    }

    public PrintingTraversal( final String indent )
    {
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

        return super.traverseEdge( relationship, path, pass );
    }

    public String getString()
    {
        return buffer.toString();
    }

}
