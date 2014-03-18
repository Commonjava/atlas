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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

/**
 * Filter used to determine which paths in a dependency graph to traverse (or discover).
 * The full dependency graph (relation dependency, not just maven-style dependency)
 * will be QUITE extensive, so a filter should be used in NEARLY ALL cases.
 * 
 * @author jdcasey
 */
public interface ProjectRelationshipFilter
{

    /**
     * Determine whether the given relationship should be traversed.
     * 
     * @param rel The relationship in question
     * @return true to allow traversal, false otherwise.
     */
    boolean accept( ProjectRelationship<?> rel );

    /**
     * Return the filter used to handle the next wave of relationships after the 
     * given one is traversed.
     * 
     * @param parent The parent relationship for the set of relationships to which
     * the return filter from this method will be applied
     * 
     * @return This instance WHENEVER POSSIBLE, but possibly a different filter 
     * if the relationship demands a shift in logic.
     */
    ProjectRelationshipFilter getChildFilter( ProjectRelationship<?> parent );

    /**
     * Retrieve a human-readable string that uniquely identifies the logic in this filter, 
     * along with any state stored in this instance.
     */
    String getLongId();

    /**
     * Retrieve a condensed version of the human-readable identity given in {@link #getLongId()}.
     * If the human-readable identity is sufficiently short (eg. "ANY"), then no
     * hashing is required.
     */
    String getCondensedId();

}
