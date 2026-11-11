# homing-cq-core

CQ checks the code of a Java project against rules, each serving a principle, such as *encapsulation* or *divide and
conquer*. What to check is declared as code: the packages, the rules and any exemptions. Three tools take the same
declaration:

- the **gate** fails the build on what it finds, as a test or from the command line;
- the **report** writes it down, as a table to search and a summary to read;
- the **bench** shows it in a window: each class, the rules it breaks, and its source.

Five basic rules come in an optional module, and you can write rules of your own.

The latest release, **0.1.0**, is on Maven Central, and the samples below use it.

---

## What you need

- **JDK 25 to run Maven and your tests.** CQ reads class files with the JDK's ClassFile API, final since JDK 24, and its
  own classes are built for Java 25. On an older JDK the build stops with `UnsupportedClassVersionError`.
- **Your own code can stay on Java 21.** Keep `maven.compiler.release` at 21: javac from JDK 25 still compiles your
  code for Java 21 and against the Java 21 API. If your build itself has to stay on JDK 21, see
  [On a JDK 21 build](#on-a-jdk-21-build).
- **Maven 3.9**, the version CQ is built and tried with.
- **A desktop**, for the bench only.

---

## Quick start: the gate in your tests

**1. Import the BOM** in your parent POM, or in the POM of the module you check. It holds every CQ artifact at one
version, so you name them without versions:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.captainssingapura-hue.homing.cq</groupId>
            <artifactId>homing-cq-bom</artifactId>
            <version>0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**2. Add the basic rules** to the module, for its tests. They bring the scanner and the gate, and no JavaFX:

```xml
<dependency>
    <groupId>io.github.captainssingapura-hue.homing.cq</groupId>
    <artifactId>homing-cq-rules-basic</artifactId>
    <scope>test</scope>
</dependency>
```

**3. Add one test class** to `src/test/java`:

```java
package com.example.app;

import com.homing_labs.cq.rules.basic.BasicRules;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Gate;
import com.homing_labs.cq.scan.ReactorSources;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityGateTest {

    private final Declaration app = new Declaration() {
        @Override
        public List<String> packages() {
            return List.of("com.example.app");
        }

        @Override
        public List<Path> sources() {
            return new ReactorSources(QualityGateTest.class).sources();
        }
    };

    @Test
    void theAppPassesTheGate() {
        assertEquals(List.of(), Gate.INSTANCE.failures(app.and(BasicRules.INSTANCE), getClass().getClassLoader()));
    }
}
```

- `packages()` names the packages to check, with every package below them.
- `sources()` says where their source is. `ReactorSources` finds the `src/main/java` of every module in your build, at
  any depth. The gate fails any class whose source it can't find.
- Test classes are never checked, so this class can sit in the package it checks.

**4. Run `mvn test`.** When nothing is found, the test passes. Otherwise JUnit lists one line per class and rule, with
that rule's findings joined by `; `:

```
com.example.app.Order NoPublicFields: PublicField[in=ClassName[binaryName=Order], name=quantity]
com.example.app.Order NoStaticMethods: StaticMethod[method=JavaMethod[owner=Order, name=of, parameters=JavaTypes[items=[JavaType[type=int]]]]]
```

---

## A qc module, for the report and the bench

The report and the bench run from Maven plugins and take declarations by class name. They are simplest in a module of
their own, which also keeps CQ and JavaFX out of your application's dependencies:

```
your-project/
├── pom.xml   imports the BOM
├── app/      your code, which knows nothing of CQ
└── qc/       depends on app, homing-cq-rules-basic and homing-cq-bench
```

**The qc module's dependencies:**

```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>app</artifactId>
        <version>1</version>
    </dependency>
    <dependency>
        <groupId>io.github.captainssingapura-hue.homing.cq</groupId>
        <artifactId>homing-cq-rules-basic</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.captainssingapura-hue.homing.cq</groupId>
        <artifactId>homing-cq-bench</artifactId>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Declare your code** as a named class in `src/main/java`. A tool given a class name takes its public static
`INSTANCE`, or else makes one with its public constructor that takes nothing:

```java
package com.example.qc;

import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.ReactorSources;

import java.nio.file.Path;
import java.util.List;

/** The code this project checks, with the sources of every module of its build. */
public record AppCode() implements Declaration {

    public static final AppCode INSTANCE = new AppCode();

    @Override
    public List<String> packages() {
        return List.of("com.example.app");
    }

    @Override
    public List<Path> sources() {
        return new ReactorSources(AppCode.class).sources();
    }
}
```

**The gate**, as a test in `src/test/java`. Declarations compose with `and`:

```java
@Test
void theAppPassesTheGate() {
    assertEquals(List.of(), Gate.INSTANCE.failures(AppCode.INSTANCE.and(BasicRules.INSTANCE),
            getClass().getClassLoader()));
}
```

**The report and the bench**, in the qc module's `<build><plugins>`. Each names the same declarations, which it
composes in order:

```xml
<!-- mvn exec:java writes the report -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.6.3</version>
    <configuration>
        <mainClass>com.homing_labs.cq.scan.Report</mainClass>
        <commandlineArgs>--out ${project.build.directory}/cq-report com.example.qc.AppCode com.homing_labs.cq.rules.basic.BasicRules</commandlineArgs>
    </configuration>
</plugin>
<!-- mvn javafx:run opens the bench -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.homing_labs.cq.bench.Bench</mainClass>
        <commandlineArgs>com.example.qc.AppCode com.homing_labs.cq.rules.basic.BasicRules</commandlineArgs>
    </configuration>
</plugin>
```

Run `mvn install` once at the top of your project, then, in `qc`:

- `mvn exec:java` writes `target/cq-report/cq-report.md`, a summary to read first, and `cq-report.tsv`, one row per
  finding, for a person or an agent to search and filter. The report never fails the build; that is the gate's work.
- `mvn javafx:run` opens the bench. Its *Rule book* lists every rule, explains it, and shows what it found. JavaFX
  comes from Maven Central with the bench, for your platform.

---

## Choosing the rules

`homing-cq-rules-basic` has five rules, one ruleset per principle:

| Ruleset | Rule | In short |
|---|---|---|
| `DivideAndConquerRules` | `TopLevelClassesWithin250Lines` | a top-level class is at most 250 effective lines |
| | `MethodsWithin50Lines` | a method or constructor is at most 50 effective lines |
| `EncapsulationRules` | `NoPublicFields` | no public instance fields |
| `BehaviourLivesOnObjectsRules` | `NoStaticMethods` | no static methods, except a program's `main` |
| `NoGlobalMutableStateRules` | `NoStaticMutableVariables` | every static field is final, and of a type known to be immutable |

`BasicRules` composes all four. Take only some by composing them instead:

```java
AppCode.INSTANCE.and(DivideAndConquerRules.INSTANCE).and(EncapsulationRules.INSTANCE)
```

The size rules suit almost any project. The last two are stricter than they sound: `NoStaticMethods` counts every
static helper, and `NoStaticMutableVariables` counts a static logger and a `static final List`.

---

## Letting a class off a rule

Sometimes a class has to break a rule, such as a field a framework writes to. An exemption lets that class off that
rule, without touching the class. It has three parts, all in the qc module.

**The excuse**: a type of its own, typed to the rule it answers, which says why by its name and its documentation:

```java
/** A form binder writes straight to the field, so it has to be public. */
public record BoundToAForm() implements Excuse<NoPublicFields> {
}
```

**The exemptions of one class:**

```java
public record OrderExemptions() implements Exemptions<Order> {

    public static final OrderExemptions INSTANCE = new OrderExemptions();

    @Override
    public Set<Exemption<Order, ?, ?>> exemptions() {
        return Set.of(new Exemption<>(Order.class, NoPublicFields.class, BoundToAForm.class));
    }
}
```

**A declaration that lists them**, composed with the rest:

```java
public record AppExemptions() implements Declaration {

    public static final AppExemptions INSTANCE = new AppExemptions();

    @Override
    public List<Exemptions<?>> exemptions() {
        return List.of(OrderExemptions.INSTANCE);
    }
}
```

```java
AppCode.INSTANCE.and(BasicRules.INSTANCE).and(AppExemptions.INSTANCE)
```

Add `com.example.qc.AppExemptions` to the plugins' `commandlineArgs` too. Then:

- the rule still runs, and what it finds is still shown, marked with the excuse; only the gate lets it through;
- the class still counts as not complying, in the report and on the bench, since its code hasn't changed;
- an exemption that no longer lets anything off is *stale* and fails the gate, so remove it when the class is fixed.

The compiler holds an exemption to its class and rule: an excuse written for another rule, or for another class, does
not compile. Exempt a top-level class, with one excuse per class and rule. A package-private class can only be named
by exemptions in its own package.

---

## Rules of your own

A rule is a `Rule<P, L>`, serving a `Principle` `P` and reading a language `L`. An `Enforcer` checks units by it, and a
`Ruleset` lists enforcers. Compose a ruleset with your code as you would `BasicRules`, and the gate, the report and the
bench take it as they take those. The five basic rules, in `homing-cq-rules-basic`, are complete examples to copy from.

---

## On a JDK 21 build

The simplest is to run Maven on JDK 25 and keep `maven.compiler.release` at 21, as above: your classes are still Java
21 classes.

If the build itself must run on JDK 21, keep CQ in a qc module and give only that module JDK 25. There are two ways:

- **Maven toolchains**: set `jdkToolchain` to version 25 on the qc module's compiler and surefire plugins, and list a
  JDK 25 in `~/.m2/toolchains.xml`. The rest of the build stays on JDK 21. The report and bench plugins would need JDK
  25 too.
- **A separate project**, built on JDK 25, that depends on your application's jar. Its `sources()` must name your
  source folders itself, as `ReactorSources` only finds the build it is in.

Neither has been tried on a JDK 21 build yet.

---

## When something goes wrong

| What you see | Why | What to do |
|---|---|---|
| `UnsupportedClassVersionError` naming a `com.homing_labs.cq` class | Maven, the tests or the IDE run on a JDK before 25 | run them on JDK 25 |
| `<class>: read without its source` | no source folder holds the class's source | compose `ReactorSources`, or name the folders in `sources()` |
| `no unit read: nothing was checked` | the packages matched nothing | check the package names, and that their module is a dependency |
| `no rule to check by: nothing was checked` | no ruleset was composed | compose `BasicRules`, or one of its rulesets |
| `NoClassDefFoundError` stops the scan | a checked class names a type that can't be loaded, often from an optional or `provided` dependency | add that dependency where the gate runs |
| `<class> <Rule>: <Excuse> is stale: …` | the exemption lets nothing off: the class was fixed, isn't scanned, or isn't checked by that rule | remove the exemption, or fix what it names |

---

## The artifacts

All under the group `io.github.captainssingapura-hue.homing.cq`:

| Artifact | What it is |
|---|---|
| `homing-cq-bom` | every artifact below at one version, to import |
| `homing-cq-rules-basic` | the five basic rules; brings the scanner, the gate, the report and the Java model |
| `homing-cq-bench` | the bench, with JavaFX 25 |
| `homing-cq-scan` | the scanner, the gate, the report and exemptions, with no UI |
| `homing-cq-core-model-principle`, `homing-cq-core-model`, `homing-cq-enforcer-model` | principles, rules, excuses and enforcers |
| `homing-cq-lang-java`, `homing-cq-lang-java-taxonomy`, `homing-cq-lang-java-lexer`, `homing-cq-lang-java-outline` | reading Java classes and their source |

---

## Building CQ

With JDK 25, at the top of this repository:

```bash
mvn install
```

This installs the development version, `LOCAL-SNAPSHOT`. To try a build of your own in a project, use that version
in place of the release.

Some of the bench's tests open windows, so the full build needs a desktop. `mvn install -DskipTests` builds without
running the tests.

`homing-cq-not-for-publish/homing-cq-demo` checks CQ's own code by the basic rules, with one exemption. In that folder,
`mvn javafx:run` opens the bench over it and `mvn exec:java` writes its report.

---

## Licence

MIT. See [LICENSE](LICENSE).
