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
package org.commonjava.maven.atlas.graph.rel;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

public interface ProjectRelationship<T extends ProjectVersionRef>
    extends Serializable
{

    int getIndex();

    RelationshipType getType();

    ProjectVersionRef getDeclaring();

    T getTarget();

    ArtifactRef getTargetArtifact();

    ProjectRelationship<T> cloneFor( final ProjectVersionRef projectRef );

    @Deprecated
    ProjectRelationship<T> selectDeclaring( SingleVersion version );

    @Deprecated
    ProjectRelationship<T> selectDeclaring( SingleVersion version, boolean force );

    @Deprecated
    ProjectRelationship<T> selectTarget( SingleVersion version );

    @Deprecated
    ProjectRelationship<T> selectTarget( SingleVersion version, boolean force );

    ProjectRelationship<T> selectDeclaring( ProjectVersionRef ref );

    ProjectRelationship<T> selectTarget( ProjectVersionRef ref );

    boolean isManaged();

    Set<URI> getSources();

    void addSource( URI source );

    void addSources( Collection<URI> sources );

    URI getPomLocation();

}
