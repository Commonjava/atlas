package org.apache.maven.graph.effective.traverse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.maven.graph.effective.filter.AnyFilter;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class PrintingTraversal
    extends AbstractFilteringTraversal
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
        super( new AnyFilter() );
        this.indent = indent;
    }

    public PrintingTraversal( final ProjectRelationshipFilter filter, final String indent )
    {
        super( filter );
        this.indent = indent;
    }

    @Override
    public boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                       final List<ProjectRelationship<?>> path, final int pass )
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

}
