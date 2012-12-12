# Atlas Graph-Oriented Artifact API

Atlas is an attempt to reimagine the artifact model that's used in Apache Maven. Starting from first principles about how artifact coordinate metadata is actually used, Atlas defines a new set of identities aimed at making different types of references to a project more explicit and intuitive. Moving beyond simple identities, Atlas also attempts to provide a simple means for capturing and working with the relationships between projects via its graphing api.

Note: This project doesn't make any attempt to provide an interface for actually resolving artifacts or loading project-relationship graphs. It's only meant to supply the model and a set of associated, useful mechanisms for traversing, transforming, and filtering relationships.

## Basic Identities

### Projects

Projects are codebases with associated development effort and long-term lifetimes. Therefore, it's inappropriate to reference a particular project using a versioned coordinate. Projects may be referenced in this way perform high-level analyses such as project interdependency, quality metrics over the span of many releases, etc.

**Projects** are identified in Atlas using `ProjectRef`  with a coordinate consisting of `groupId:artifactId`.

ProjectRefs are almost never appropriate in Maven. They can be used in plugin references (DEPRECATED), and that's about it. Maven allows specifying a plugin by groupId and artifactId, and it will resolve the version from the LATEST meta-version available in the repository. In practice, this is dangerous, since it can lead to unpredictable builds depending on the state of your local repository and which remote repositories you use. It can also lead to irreproducible builds, since the LATEST version for a plugin will change over time.

### Project Releases

The whole point of a project is to release code for use in the wider world. When it does this, the release gets a particular version that fits into a broader context of releases for that project. For instance, releases should be sortable in terms of what came first vs. later. Their versions often also make certain claims about the quality of the release, such as `alpha`, `beta`, etc. However, it's important to note that a project release probably still references a set of artifacts. 

In Maven, a project's release does have at least one dependable, concrete file associated with it: a POM. Often, if the Maven project isn't simply a parent POM, it will also have an implied, unclassified jar artifact associated with it as well, as the release's "main" artifact. This implied main-artifact jar is what allows most Maven dependency declarations get away with simply referencing the release of another project. If any other artifact from a project release is required, then a simple reference to the release is inappropriate.

**Releases** are identified in Atlas using `ProjectVersionRef` with a coordinate consisting of `groupId:artifactId:version`.

Common places to find ProjectVersionRef in Maven POMs are:

  - Parent declaration
  - Plugin declaraion
  - Build extension declaration
  - Dependency declaration (**ONLY** when the main artifact is being referenced.) This is really an ArtifactRef that relies on default values for classifier and type.
  - Plugin-level dependency declaration (again, **ONLY** when the main artifact is being referenced.) This is really an ArtifactRef that relies on default values for classifier and type.

### Release Artifacts

When a project performs a release, it produces something tangible that others can consume, either as a dependency in their own project (as a library), or directly (as an application). The products of a particular release are called artifacts, and normally correspond to actual files you can download from a Maven repository. Artifacts that vary from the main one for a project (normally a jar, war, etc.) are distinguished by their *type* and *classifier*. *Classifier* doesn't have to be specified when producing an extra artifact, and *type* defaults to `jar`. The main requirement for non-main artifacts is to avoid colliding with the main artifact, whose *classifier* is `null` and *type* is `jar`.

Technically speaking, dependency declarations in Maven use five coordinate parts: groupId, artifactId, version, type, and classifier. The reason most declarations can get by with a ProjectVersionRef (release coordinate) is because of the default values for type and classifier.

**Artifacts** are identified in Atlas using `ArtifactRef` with a coordinate consisting of `groupId:artifactId:version:type[:classifier]` where the default value of `type` is `jar`.

The only common places to find ArtifactRef in Maven POMs are:

  - Dependency declaration
  - Plugin-level dependency declaration

## Version Parsing

Atlas also contains a javacc grammar for parsing versions. This is relatively stable for most mainstream version schemes, but some outliers may still cause problems.

The associated version-sorting implementation tries to follow the conventions described in: https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning with a few notable exceptions. Wherever two schemes clash and are unlikely to co-exist for a single project, their direct comparison may differ from that described in the wiki. This was done for the sake of simplicity, and seems like a reasonable compromise for now.

## Relationships

Maven's POMs express at least five different types of relationships to other projects' artifacts:

  - Parent
  - Dependency
  - Extension
  - Plugin
  - Plugin-Level Dependency (dependencies declared in the plugin section, as project-specific add-ons to the plugin classpath)

Each of these has different meaning in the build process, and may have different sets of associated information that help fine-tune when they're used even outside the build process. Each relationship type contains a `ProjectVersionRef` pointing to the declaring project version (which corresponds to the POM in which the relationship was expressed). 

Atlas captures each of these relationships as a variant of its foundational `ProjectRelationship` class:

  - **ParentRelationship** - Contains a basic `ProjectVersionRef` pointing to the project POM it references. Since POMs don't have type or classifier associated with them in Maven, there's no need to specify anything more than the project release for this relationship.
  - **DependencyRelationship** - Contains an `ArtifactRef` for the dependency artifact it references, along with `scope` (default: `compile`) and a flag for whether the dependency was specified in the `<dependencyManagement/>` section or not.
  - **ExtensionRelationship** - Contains a basic `ProjectVersionRef` for the extension artifact it references. Maven assumes that only main artifacts with `type` of `jar` are available as build extensions.
  - **PluginRelationship** - Contains a `ProjectVersionRef` for the plugin it references, along with a flag for whether the plugin was specified in the `<pluginManagement/>` section or not.
  - **PluginDependencyRelationship** - Contains a `ProjectRef` to the plugin under which it was declared, along with the `ArtifactRef` for what dependency artifact the declaration references. This relationship is different from `DependencyRelationship` because it doesn't store `scope` (scope is meaningless here). It also contains a flag for whether the plugin was specified in the `<pluginManagement/>` section or not.

## Collections of Relationships

The classes in this section are usually prefixes with **'E'**. This is meant to mark these classes as dealing with effective POMs. That is, the classes and relationships here don't deal with partial declarations that may be filled out through inheritance, profile activation, etc. It's assumed for now that these classes are working with complete (validatable) expressions of relationships.

### Collection Key

Each collection is keyed by `EProjectKey` which is a composite of `ProjectVersionRef` and `EProjectFacts`. `EProjectFacts` stores facts about the conditions under which the relationships were captured. Currently, it is only capable of tracking the list of profiles that were active at the time. Things like the list of active profiles can have a profound effect on the list of relationships that exist, so it's critical that the key for each of these relationship collections include these facts.

**NOTE:** The usage of / logic related to EProjectFacts is still VERY immature. For initial development purposes, its existence has largely been ignored (defaulted to an empty set of profiles). This is mainly a TODO for future work.

### Collection Types

Atlas currently defines three basic collections of relationships:

  - **EProjectRelationships** - This roughly corresponds to the relationships expressed in a POM, given a certain list of active profiles (see above, regarding EProjectFacts).
  - **EProjectGraph** - This corresponds to the entire graph of relationships flowing out of a given POM, potentially including all types of relationships for everything in the transitive closure. Graphs can be transformed using filters to achieve more focused results.
  - **EProjectWeb** - This is similar to EProjectGraph, but has no single root POM. It merely expresses the interrelationship between a set of projects, and can be thought of as a super-graph of sorts, where EProjectGraph instances may be built by selecting one project in the web and extracting the graph of relationships to that project.

### Filters

Atlas provides an interface and set of basic implementations for filtering individual `ProjectRelationship` instances in a graph. They're useful for traversing or transforming networks of relationships, or could be useful outside these contexts as well.

### Traversal

Atlas also provides an interface, two abstract base classes, and some basic implementations for traversing a network of relationships. This is done by instantiating the `ProjectNetTraversal` in question, then calling `traverse(..)` on the `EProjectGraph` or `EProjectWeb` and passing in the traversal instance. Once the `traverse(..)` completes, the traversal instance should provide extra methods to access whatever information it was designed to accumulate.

The `ProjectNetTraversal` provides callback methods for starting and ending graph traversal, and for ending edge traversal. Additionally, and most critically, it provides a method that allows the instance to veto the traversal of any given edge. This is where most filters are used, and the simplest way to filter in a traversal is by subclassing `AbstractFilteringTraversal`. Along with these callbacks and acceptance methods, each traversal has the option of specifying how many times it needs to traverse the graph, and for each iteration, whether it needs to use depth-first or breadth-first traversal to achieve its ends.

### Transformation

Finally, Atlas provides an interface and some basic implementations for transforming an `EProjectGraph` instance into another one, by applying a filter (see above). `ProjectGraphTransformer` extends `ProjectNetTraversal`, and adds the method `getTransformedGraph()`. The implementation, `FilteringGraphTransformer` extends `AbstractFilteringTraversal` and accepts a `ProjectRelationshipFilter` instance. You use these transformers by passing them into the `EProjectGraph.traverse(..)` method, just like any other traversal. However, after traversal completes, you can retrieve the transformed graph via the `getTransformedGraph()` method.

