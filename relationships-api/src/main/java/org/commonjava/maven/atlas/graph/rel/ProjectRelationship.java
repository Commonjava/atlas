/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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

    // FIXME: Fix the api to allow relocations!
    ProjectRelationship<T> selectDeclaring( SingleVersion version );

    // FIXME: Fix the api to allow relocations!
    ProjectRelationship<T> selectDeclaring( SingleVersion version, boolean force );

    // FIXME: Fix the api to allow relocations!
    ProjectRelationship<T> selectTarget( SingleVersion version );

    // FIXME: Fix the api to allow relocations!
    ProjectRelationship<T> selectTarget( SingleVersion version, boolean force );

    boolean isManaged();

    Set<URI> getSources();

    void addSource( URI source );

    void addSources( Collection<URI> sources );

    URI getPomLocation();

}
