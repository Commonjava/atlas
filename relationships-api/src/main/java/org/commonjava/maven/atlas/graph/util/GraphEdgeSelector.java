package org.commonjava.maven.atlas.graph.util;

import java.util.List;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface GraphEdgeSelector
{

    List<ProjectRelationship<?>> getSortedOutEdges( GraphView view, ProjectVersionRef node );

}
