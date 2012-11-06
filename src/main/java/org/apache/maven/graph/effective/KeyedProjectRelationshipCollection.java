package org.apache.maven.graph.effective;

import org.apache.maven.graph.effective.ref.EProjectKey;

public interface KeyedProjectRelationshipCollection
    extends EProjectRelationshipCollection
{

    EProjectKey getKey();

}
