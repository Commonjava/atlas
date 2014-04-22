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
package org.commonjava.maven.atlas.graph.traverse.print;

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface StructureRelationshipPrinter
{
    void print( ProjectRelationship<?> relationship, ProjectVersionRef targetOverride, StringBuilder builder,
                Map<String, Set<ProjectVersionRef>> labels, int depth, String indent );

    void printProjectVersionRef( ProjectVersionRef targetArtifact, StringBuilder builder, String targetSuffix,
                                 Map<String, Set<ProjectVersionRef>> labels, Set<String> localLabels );
}
