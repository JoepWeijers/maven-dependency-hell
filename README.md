# Maven Dependency Hell examples
This project illustrates some of the forms of dependency clashes you may run into when using Apache Maven.

## The subject dependencies:
* **Guava**: Google's well known convenience library for Java. Guava has a steady release cycle and contains breaking changes between major versions.
* **Project A** depends on Guava version 10.0.1. This project calls two methods in Guava: `Equivalences.identity()` (present in Guava 10.0.1 but not in 20.0) and `ImmutableMap.of()` (present in both Guava 10.0.1 and 20.0).
* **Project B** depends on Project C.
* **Project C** depends on Guava version 20.0. This project calls `GraphBuilder.undirected().build()`, which is present in Guava 20.0, but not in 10.0.1.

## The first problem: Clashing Dependencies
Maven's dependencies work transitively. This means that it will add all dependencies of my dependencies on the classpath. While not having to explicitly specify all dependencies is very convenient, it is also the cause of some problems which I will illustrate here. Load up this repo into your workspace and following along with the explanation.

Suppose we have a project that has a dependency on both Project A and Project B, called [h1_problem-clashing-dependencies](h1_problem-clashing-dependencies/pom.xml). This project has a transitive dependency on Guava 10.0.1 through it's dependency on A. It also has a transitive dependency on Guava 20.0 through B, since B depends on C and C depends on Guava 20.0. Which version of Guava will end up at our classpath?

We can determine the chosen Guava in the Dependency Hierarchy view in Eclipse. Open the `pom.xml` in an Eclipse with [M2E](https://www.eclipse.org/m2e/) installed. Alternatively we can use the `mvn dependency:tree -Dverbose` command in a shell. The output is as follows:
```
[INFO] com.topdesk.maven-hell:h1_problem-clashing-dependencies:jar:0.0.1-SNAPSHOT
[INFO] +- com.topdesk.maven-hell:A-depends-on-guava-10:jar:0.0.1-SNAPSHOT:compile
[INFO] |  \- com.google.guava:guava:jar:10.0.1:compile
[INFO] |     \- com.google.code.findbugs:jsr305:jar:1.3.9:compile
[INFO] \- com.topdesk.maven-hell:B-depends-on-C:jar:0.0.1-SNAPSHOT:compile
[INFO]    \- com.topdesk.maven-hell:C-depends-on-guava-20:jar:0.0.1-SNAPSHOT:compile
[INFO]       \- (com.google.guava:guava:jar:20.0:compile - omitted for conflict with 10.0.1)
```

We can see that Guava 20.0 is omitted for a conflict with 10.0.1. In general Maven will select the 'nearest' defined version of a dependency. In this case the path to 10.0.1 is only two steps, while Guava 20.0 is three steps. Hence 10.0.1 is selected as version.

But this might not be the version we want. If we try to run the `main` method of the class [H1CallingGuava20.java](h1_problem-clashing-dependencies/src/main/java/com/topdesk/maven_hell/problem/H1CallingGuava20.java) we get the following error:
```
Exception in thread "main" java.lang.NoClassDefFoundError: com/google/common/graph/GraphBuilder
	at com.topdesk.maven_hell.c.ThisClassInCDependsOnGuava20.methodOnlyInGuava20(ThisClassInCDependsOnGuava20.java:7)
	at com.topdesk.maven_hell.b.ThisClassInBDependsOnGuava20.methodOnlyInGuava20(ThisClassInBDependsOnGuava20.java:7)
	at com.topdesk.maven_hell.problem.CallingGuava20.main(CallingGuava20.java:7)
Caused by: java.lang.ClassNotFoundException: com.google.common.graph.GraphBuilder
	... 7 more
```

The main method in `H1CallingGuava20` requires Guava 20.0 at runtime on the classpath, since it calls `GraphBuilder.undirected().build()` and that class is not present in Guava 10.0.1.

Note that this is an error that appears at runtime, since we are talking about the classpath that Maven provides through transitive dependencies at runtime. So this example does not show any errors during compilation. The code in the project only directly references project B and is compiled against an already compiled version of B.

How can we persuade Maven to use the version 20.0 of Guava?

## Solution 1: use a direct dependency
We can use our knowledge about Maven dependency resolution to our advantage. If we make sure that a dependency declaration of Guava 20.0 is closer than 10.0.1, we are good to go. So we added a direct dependency on Guava 20.0 in [h2_solution1-direct-dependency](h2_solution1-direct-dependency/pom.xml):
```
<dependencies>
  ...
  <!-- Solution 1: Use a direct dependency -->
  <dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>20.0</version>
  </dependency>
</dependencies>
```

We can verify that Guava 20.0 now wins the conflict by looking at the dependency tree again with `mvn dependency:tree -Dverbose`:
```
[INFO] com.topdesk.maven-hell:h2_solution1-direct-dependency:jar:0.0.1-SNAPSHOT
[INFO] +- com.topdesk.maven-hell:A-depends-on-guava-10:jar:0.0.1-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:10.0.1:compile - omitted for conflict with 20.0)
[INFO] +- com.topdesk.maven-hell:B-depends-on-C:jar:0.0.1-SNAPSHOT:compile
[INFO] |  \- com.topdesk.maven-hell:C-depends-on-guava-20:jar:0.0.1-SNAPSHOT:compile
[INFO] |     \- (com.google.guava:guava:jar:20.0:compile - omitted for conflict with 10.0.1)
[INFO] \- com.google.guava:guava:jar:20.0:compile
```

And indeed we can run the `main` method in [H2CallingGuava20.java](h2_solution1-direct-dependency/src/main/java/com/topdesk/maven_hell/problem/H1CallingGuava20.java) without a `ClassNotFoundException`.


But it is pretty weird to say that our project has a direct dependency on Guava, while we only have a transitive dependency on Guava. And the Maven dependency analyzer agrees with this sentiment. Running `mvn dependency:analyze` will emit the following warning:
```
[WARNING] Unused declared dependencies found:
[WARNING]    com.google.guava:guava:jar:20.0:compile
```

So this is not a nice solution. Can we do better?

## Solution 2: Dependency Management
The dependency management block in Maven allows you to control which version of a dependency to use. It works for transitive dependencies, but also allows you to omit the version declaration if you depend directly on a library in the `<dependencies>` section. The `pom.xml` in  [h3_solution2-dependency-management](h3_solution2-dependency-management/pom.xml) shows the usage of the dependency management section:
```
<!-- Solution 2: Use dependency management -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>20.0</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

This solves the warning in `mvn dependency:analyze`:
```
[INFO] --- maven-dependency-plugin:2.10:analyze (default-cli) @ h3_solution2-dependency-management ---
[INFO] No dependency problems found
```

And `mvn dependency:tree -Dverbose` will now let us know that the version is managed:
```
[INFO] com.topdesk.maven-hell:h3_solution2-dependency-management:jar:0.0.1-SNAPSHOT
[INFO] +- com.topdesk.maven-hell:A-depends-on-guava-10:jar:0.0.1-SNAPSHOT:compile
[INFO] |  \- com.google.guava:guava:jar:20.0:compile (version managed from 10.0.1)
[INFO] \- com.topdesk.maven-hell:B-depends-on-C:jar:0.0.1-SNAPSHOT:compile
[INFO]    \- com.topdesk.maven-hell:C-depends-on-guava-20:jar:0.0.1-SNAPSHOT:compile
[INFO]       \- (com.google.guava:guava:jar:20.0:compile - version managed from 10.0.1; omitted for duplicate)
```

This solves our dependency clash for now. But people will probably add new dependencies over time. Dependency clashes are typically runtime problems, so how can we prevent them from turning into `ClassNotFoundException`s in production? Having automated tests helps, but it is unlikely that you have 100% percent test coverage.

## Solution 3: Maven Enforcer plugin
At TOPdesk we adopted a zero-tolerance policy for duplicate dependencies. We use the [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/) in our Continuous Integration build to break the build if our transitive dependencies versions do not converge to a single version. We add this configuration in [h4_solution3-maven-enforcer-plugin_breaks-build](h4_solution3-maven-enforcer-plugin_breaks-build/pom.xml):

```
<!-- Solution 3: Use the enforcer plugin -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-enforcer-plugin</artifactId>
      <version>1.4</version>
      <executions>
        <execution>
          <id>enforce-banned-dependencies</id>
          <goals>
            <goal>enforce</goal>
          </goals>
          <configuration>
            <rules>
              <DependencyConvergence />
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Running a `mvn compile` command with this configuration will result in a failed build:
```
[WARNING] Rule 0: org.apache.maven.plugins.enforcer.DependencyConvergence failed with message:
Failed while enforcing releasability the error(s) are [
Dependency convergence error for com.google.guava:guava:10.0.1 paths to dependency are:
+-com.topdesk.maven-hell:h4_solution3-maven-enforcer-plugin_breaks-build:0.0.1-SNAPSHOT
  +-com.topdesk.maven-hell:A-depends-on-guava-10:0.0.1-SNAPSHOT
    +-com.google.guava:guava:10.0.1
and
+-com.topdesk.maven-hell:h4_solution3-maven-enforcer-plugin_breaks-build:0.0.1-SNAPSHOT
  +-com.topdesk.maven-hell:B-depends-on-C:0.0.1-SNAPSHOT
    +-com.topdesk.maven-hell:C-depends-on-guava-20:0.0.1-SNAPSHOT
      +-com.google.guava:guava:20.0
```

The plugin detects that Guava 10.0.1 and 20.0 are both on the classpath and thus fails the build.

We use Maven dependency exclusions to exclude the transitive dependencies we don't want. Looking at the code [H5CallingGuava10](h5_solution3-maven-enforcer-plugin_fixed/src/main/java/com/topdesk/maven_hell/problem/H5CallingGuava10.java) and [H5CallingGuava20](h5_solution3-maven-enforcer-plugin_fixed/src/main/java/com/topdesk/maven_hell/problem/H5CallingGuava20.java), we can determine that Guava 20.0 is the desired version. So we want to exclude Guava 10.0.1, so we add the following exclusion, as shown in [h5_solution3-maven-enforcer-plugin_fixed](h5_solution3-maven-enforcer-plugin_fixed/pom.xml):

```
<dependency>
  <groupId>com.topdesk.maven-hell</groupId>
  <artifactId>A-depends-on-guava-10</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <exclusions>
    <exclusion>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

Running `mvn compile` will now result in a 'Build Success', so the enforcer plugin is satisfied.

Note that you sometimes run into a dependency that has different versions of a dependency in its dependency tree, i.e. it has no dependency convergence itself. You unfortunately can't specify the version to exclude with the exclusions section. Adding that section will exclude all versions of that transitive dependency. This can sometimes force you to take a direct dependency on the excluded dependency, to ensure that at least a version of that dependency ends up at your classpath.

Excluding dependencies should not be taken lightly, since they in itself can be the source of `ClassNotFoundException`s. Your dependency has been compiled against a certain version of a library. Providing another version of that dependency at runtime will only work if it is binary compatible for all calls to that library. Binary compatible means that the API of the two version must be the same and with API here we mean class names, method signatures and public field names.

There are tools that can show the differences in API between libraries. And there are tools that try to determine all usages of a dependency (e.g. `mvn dependency:analyze`) but determining whether you can safely swap out different versions of a library is nearly impossible. These tools for example don't work well with regards to reflection or service loading using SPI.

We have achieved a small improvement so far: we went from not knowing whether runtime classpath problems would occur, to localizing where they might occur. But there are some more problems ahead. For example: what if there is no 'correct' version of a dependency?

Project [h6_problem-non-binary-compatible-dependencies](h6_problem-non-binary-compatible-dependencies/pom.xml) illustrates a project which has no version of a dependency that suits all transitive requirements. [H6CallingGuava10](h6_problem-non-binary-compatible-dependencies/src/main/java/com/topdesk/maven_hell/problem/H6CallingGuava10.java) requires Guava 10.0.1 on the classpath at runtime and [H6CallingGuava20](h6_problem-non-binary-compatible-dependencies/src/main/java/com/topdesk/maven_hell/problem/H6CallingGuava20.java) needs Guava 20.0. There is no `<exclusion>` that will prevent `ClassNotFoundException`s.

## Solution 4: Shading

To solve this issue, we preferably update the dependency on Guava in project A. If this is not possible we can also change the artifact generated in project A to include the Guava dependency in a so-called 'fat jar'. To achieve this, we modify project A to use the shade plugin as shown in [h7_solution4-create-a-shaded-jar-of-A-depends-on-guava-10](h7_solution4-create-a-shaded-jar-of-A-depends-on-guava-10/pom.xml):
```
<build>
	<plugins>
		<plugin>
			<!-- Solution 4: use shading -->
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-shade-plugin</artifactId>
			<version>3.1.0</version>
			<executions>
				<execution>
					<phase>package</phase>
					<goals>
						<goal>shade</goal>
					</goals>
					<configuration>
						<relocations>
							<relocation>
								<pattern>com.google</pattern>
								<shadedPattern>com.topdesk.maven-hell.shaded.com.google</shadedPattern>
							</relocation>
						</relocations>
					</configuration>
				</execution>
			</executions>
		</plugin>
	</plugins>
</build>
```

Instead of creating a regular jar file with only the contents of your project, the shade plugin also unpacks all your dependencies. The trick we use here to make sure that we can have Guava version 10.0.1 on the classpath, next to Guava 20.0, without interfereing is called relocation. We relocate Guava in our jar by renaming all fully qualified class names that start with `com.google` to `com.topdesk.maven-hell.shaded.com.google`. This also updates all calls to any Guava class in your jar.

After running `mvn install` on this project, we update our example project to depend on this shaded version of A, as shown in [h8_solution4-use-a-shaded-jar](h8_solution4-use-a-shaded-jar/pom.xml):
```
<dependencies>
	<!-- Solution 4: use shading -->
	<dependency>
		<groupId>com.topdesk.maven-hell</groupId>
		<artifactId>h7_solution4-create-a-shaded-jar-of-A-depends-on-guava-10</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>
	...
</dependencies>
```

If you are looking at project H8 in Eclipse, you need to right click -> Maven -> Disable workspace resolution. Eclipse is clever and sees that you depend on project H7, which it has in its workspace. Eclipse will then use the classfiles that it compiled itself, instead of the shaded jar from your local Maven repository.

Both [H8CallingGuava10](h8_solution4-use-a-shaded-jar/src/main/java/com/topdesk/maven_hell/problem/H8CallingGuava10.java) and [H8CallingGuava20](h8_solution4-use-a-shaded-jar/src/main/java/com/topdesk/maven_hell/problem/H8CallingGuava20.java) now run correctly. If we look at the dependency tree, we see that there is no transitive dependency on Guava 10.0.1 anymore. Maven has used the [dependency reduced pom of H7](h7_solution4-create-a-shaded-jar-of-A-depends-on-guava-10/dependency-reduced-pom.xml), generated by the shade plugin.

Shading has several downsides:
* In our case it is pretty easy to change Project A, since it is our project. If this would be a closed source project, then you can't change it to use the shade plugin.
* Every time Project A or its dependencies change, you need to repackage everything.
* Shading does not always work. Again in the case of reflection and service loading with SPI. There is a transformer to change SPI manifest files, but that isn't flawless.
* Shading is slow. The plugin has to iterate through all your class files and update them.
* Your application and your classpath can become really big. In this example we already get two versions of Guava on our classpath instead of only one. In the big Java monolith at our company we have about 50 times a transitive dependency on Guava. Imagine that each one of those would be shaded, even though Guava is only 2.7 MB, our software package would be almost 150 MB bigger.

## Conclusion
These are some of the forms of Maven's Dependency Hell that you may encounter. It is a hard problem that not always has a satisfying solution, but I hope you now have a better understanding of your options when you are faced with the Maven Dependency Hell.
