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
