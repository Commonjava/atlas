/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.filter;

import java.io.Serializable;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

/**
 * Filter used to determine which paths in a dependency graph to traverse (or discover).
 * The full dependency graph (relation dependency, not just maven-style dependency)
 * will be QUITE extensive, so a filter should be used in NEARLY ALL cases.
 * 
 * @author jdcasey
 */
public interface ProjectRelationshipFilter
    extends Serializable
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

    boolean includeManagedRelationships();

    boolean includeConcreteRelationships();

    Set<RelationshipType> getAllowedTypes();

}
