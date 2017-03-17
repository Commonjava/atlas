/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.graph.jackson;

import com.fasterxml.jackson.core.io.SerializedString;

public final class SerializationConstants
{

    private SerializationConstants()
    {
    }

    public static final String SOURCE_URIS = "source-uris";

    public static final String SOURCE_URI = "source-uri";

    public static final String POM_LOCATION_URI = "pom-location-uri";

    public static final String PROJECT_VERSION = "gav";

    public static final String GAV = PROJECT_VERSION;

    public static final String RELATIONSHIP_TYPE = "type";

    public static final String DECLARING_REF = "declaring";

    public static final String TARGET_REF = "target";

    public static final String INDEX = "idx";

    public static final String INHERITED = "inherited";

    public static final String MANAGED = "managed";

    public static final String MIXIN = "mixin";

    public static final String OPTIONAL = "optional";

    public static final String REPORTING = "reporting";

    public static final String SCOPE = "scope";

    public static final String PLUGIN_REF = "plugin";

    public static final String JSON_VERSION = "jsonVersion";

    public static final int CURRENT_JSON_VERSION = 1;

    public static final String EPROJECT_KEY = "ekey";

    public static final String RELATIONSHIPS = "relationships";

    public static final String EPROFILES = "eprofiles";

    public static final String CYCLES = "cycles";

    public static final String WEB_ROOTS = "gavs";

    public static final String GAVS = WEB_ROOTS;

}
