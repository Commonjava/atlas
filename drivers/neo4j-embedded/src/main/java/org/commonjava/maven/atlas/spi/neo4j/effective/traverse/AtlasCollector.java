package org.commonjava.maven.atlas.spi.neo4j.effective.traverse;

import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.Evaluator;

@SuppressWarnings( "rawtypes" )
public interface AtlasCollector<T>
    extends Evaluator, PathExpander, Iterable<T>
{

}
