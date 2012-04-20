package org.apache.maven.pgraph.version.part;

public interface VersionPart<T>
    extends Comparable<VersionPart<?>>
{

    String renderStandard();

    T getValue();

}
