package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class SelectionInfo
{
    private final Node v, s;

    private final Relationship vr, sr;

    public SelectionInfo( final Node v, final Relationship vr, final Node s, final Relationship sr )
    {
        this.v = v;
        this.vr = vr;
        this.s = s;
        this.sr = sr;
    }

    public Node getVariable()
    {
        return v;
    }

    public Node getSelected()
    {
        return s;
    }

    public Relationship getVariableRelationship()
    {
        return vr;
    }

    public Relationship getSelectedRelationship()
    {
        return sr;
    }
}