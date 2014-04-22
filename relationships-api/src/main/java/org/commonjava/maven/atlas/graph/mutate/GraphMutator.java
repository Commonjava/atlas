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
package org.commonjava.maven.atlas.graph.mutate;

import java.io.Serializable;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

/**
 * Mechanism for selecting alternative relationships (especially by changing the
 * target artifact/project), usually making use of version-management information
 * injected by the user or accumulated during graph traversal.
 * 
 * Child instances allow for shifts in mutator logic according to encounters with 
 * different types of relationships. They also allow incorporation of new 
 * version-management information by embedding new instances of 
 * {@link VersionManager}.
 * 
 * @author jdcasey
 */
public interface GraphMutator
    extends Serializable
{

    /**
     * Alter the relationship to be traversed next, using whatever mutation logic
     * this class incorporates. For example, managed versions (a la &lt;dependencyManagement/&gt;)
     * 
     * @param rel The relationship to process.
     * @param path The path leading to this selection, which may contain management information, etc. 
     * @param view The view (context) in which this mutation is being made
     * @return The alternative relationship, or the given one if no mutation 
     * takes place.
     */
    ProjectRelationship<?> selectFor( ProjectRelationship<?> rel, GraphPath<?> path, GraphView view );

    /**
     * If necessary, create a new mutator instance to handle the next wave of 
     * relationships resulting from traversal of the given relationship.
     * 
     * @param rel The relationship that will be traversed next, for which mutator logic is needed.
     * @param view The view (context) in which this mutation is being made
     * 
     * @return  This instance WHEREVER POSSIBLE, or a new mutator instance to 
     * encapsulate changing logic or metadata. NEVER Null. Decisions about 
     * whether to proceed should be handled via {@link ProjectRelationshipFilter}, 
     * not here.
     */
    GraphMutator getMutatorFor( ProjectRelationship<?> rel, GraphView view );

    /**
     * Retrieve a human-readable string that uniquely identifies the logic in this mutator, 
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
