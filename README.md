# Atlas Graph-Oriented Artifact API

Atlas is an attempt to reimagine the artifact model that's used in Apache Maven. Starting from first principles about how artifact coordinate metadata is actually used, Atlas defines a new set of identities aimed at making different types of references to a project more explicit and intuitive. Moving beyond simple identities, Atlas also attempts to provide a simple means for capturing and working with the relationships between projects via its graphing api.

Note: This project doesn't make any attempt to provide an interface for actually resolving artifacts or discovering project-relationship graphs. It's only meant to supply the model and a set of associated, useful mechanisms for traversing, transforming, and filtering relationships.

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

  - Parent declaration (this is an implied ArtifactRef, with `type == pom` and `classifier == null`)
  - Plugin declaraion (this is an implied ArtifactRef, with `type == maven-plugin` and `classifier == null`)
  - Build extension declaration (this is an implied ArtifactRef, with `type == jar` and `classifier == null`)
  - Dependency declaration (**ONLY** when the main artifact is being referenced.) This is really an ArtifactRef that relies on default values `type == jar` and `classifier == null`.
  - Plugin-level dependency declaration (again, **ONLY** when the main artifact is being referenced.) This is really an ArtifactRef that relies on default values `type == jar` and `classifier == null`.

### Release Artifacts

When a project performs a release, it produces something tangible that others can consume, either as a dependency in their own project (as a library), or directly (as an application). The products of a particular release are called artifacts, and normally correspond to actual files you can download from a Maven repository. Artifacts that vary from the main one for a project (normally a jar, war, etc.) are distinguished by their *type* and *classifier*. *Classifier* doesn't have to be specified when producing an extra artifact, and *type* defaults to `jar`. The main requirement for non-main artifacts is to avoid colliding with the main artifact, whose *classifier* is `null` and *type* is `jar`.

Technically speaking, dependency declarations in Maven use five coordinate parts: groupId, artifactId, version, type, and classifier. The reason most declarations can get by with a ProjectVersionRef (release coordinate) is because of the default values for type and classifier.

**Artifacts** are identified in Atlas using `ArtifactRef` with a coordinate consisting of `groupId:artifactId:version:type[:classifier]` where the default value of `type` is `jar`.

The only common places to find full-blown ArtifactRef's in Maven POMs are:

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

### Collection Types

Atlas currently defines three basic collections of relationships:

  - **EProjectRelationships** - This roughly corresponds to the relationships expressed in a POM, given a certain list of active profiles (see above, regarding EProjectFacts).
  - **EProjectGraph** - This corresponds to the entire graph of relationships flowing out of a given POM, potentially including all types of relationships for everything in the transitive closure. Graphs can be transformed using filters to achieve more focused results.
  - **EProjectWeb** - This is similar to EProjectGraph, but has no single root POM. It merely expresses the interrelationship between a set of projects, and can be thought of as a super-graph of sorts, where EProjectGraph instances may be built by selecting one project in the web and extracting the graph of relationships to that project.

### Filters

Atlas provides an interface and set of basic implementations for filtering individual `ProjectRelationship` instances in a graph. They're useful for traversing or transforming networks of relationships, or just to directly constrain the dependency graph instance returned from the database.

### Traversal

Atlas also provides an interface, two abstract base classes, and some basic implementations for traversing a network of relationships. This is done by instantiating the `ProjectNetTraversal` in question, then calling `traverse(..)` on the `EProjectGraph` or `EProjectWeb` and passing in the traversal instance. Once the `traverse(..)` completes, the traversal instance should provide extra methods to access whatever information it was designed to accumulate.

The `ProjectNetTraversal` provides callback methods for starting and ending graph traversal, and for ending edge traversal. Additionally, and most critically, it provides a method that allows the instance to veto the traversal of any given edge. This is where most filters are used, and the simplest way to filter in a traversal is by subclassing `AbstractFilteringTraversal`. Along with these callbacks and acceptance methods, each traversal has the option of specifying how many times it needs to traverse the graph, and for each iteration, whether it needs to use depth-first or breadth-first traversal to achieve its ends.

### The Graph Database

Atlas stores relationship information in a single, global database that covers all relationships regardless of where they were discovered from, and from what part of the POM. The source URI and pom location are noted for each relationship to allow filtering later. 

Since project releases in Maven are designed to be immutable, a dependency graph database composed entirely of artifacts released into public repositories should not include any overlapping information for release-level versions of artifacts. However, when snapshots are included, or the database includes staging repositories and the like, depgraph information may become overlaid in the database, producing different output depending on which sources your choices: restricting the set of source locations, or selecting specific timestamped versions for snapshots can produce variable results. 

For this reason, it may not be enough to work with the unconstrained relationship data available in the graph db.

### Workspaces

Atlas provides durable workspaces to address the ambiguity that can build up in the graph db. These workspaces allow you to constrain results from the database in three dimensions:

- source URI (the location from which the relationship was discovered)
- POM location (allowing relationships from profiles with certain names, for instance)
- version selection (allowing selection of snapshots, ranges, and other variable versions down to a single, concrete version)

Since the workspace is durable, you have the option of building up a very sophisticated set of controls to tailor the output you want over time. Combined with the right filter, you can answer very detailed questions without even doing an explicit `traverse(..)` call at all. Atlas provides some basic CRUD support for workspaces; just enough to support durability.

### EGraphManager 

Most of what you need in terms of working with graphs of relationships (and workspaces for these graphs) can be done via the `org.commonjava.maven.atlas.effective.EGraphManager` class. This is designed to be a simple entry point into the graph database system, which abstracts any need to interact with the database driver beyond constructing it in the first place.

### Database Drivers

Atlas currently supports two different drivers for its dependency-graph database: Jung, which is an in-memory implementation, and Neo4J, which is backed by Lucene and written to disk. Selection between these drivers depends on your specific needs.