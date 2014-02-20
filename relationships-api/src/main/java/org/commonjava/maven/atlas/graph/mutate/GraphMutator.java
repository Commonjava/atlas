package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
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
{

    /**
     * Alter the relationship to be traversed next, using whatever mutation logic
     * this class incorporates. For example, managed versions (a la &lt;dependencyManagement/&gt;)
     * 
     * @param rel The relationship to process.
     * @return The alternative relationship, or the given one if no mutation 
     * takes place.
     */
    ProjectRelationship<?> selectFor( ProjectRelationship<?> rel );

    /**
     * If necessary, create a new mutator instance to handle the next wave of 
     * relationships resulting from traversal of the given relationship.
     * 
     * @param rel The relationship that will be traversed next, for which mutator logic is needed.
     * 
     * @return  This instance WHEREVER POSSIBLE, or a new mutator instance to 
     * encapsulate changing logic or metadata. NEVER Null. Decisions about 
     * whether to proceed should be handled via {@link ProjectRelationshipFilter}, 
     * not here.
     */
    GraphMutator getMutatorFor( ProjectRelationship<?> rel );

}
