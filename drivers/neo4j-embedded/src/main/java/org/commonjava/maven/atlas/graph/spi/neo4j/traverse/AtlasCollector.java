package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.Evaluator;

@SuppressWarnings( "rawtypes" )
public interface AtlasCollector<T>
    extends Evaluator, PathExpander, Iterable<T>
{

}
