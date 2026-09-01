package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.mintychochip.container.Currency;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.event.EventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Guards pure {@code modularjobs-api} and {@code modularjobs-common} sources. */
class ArchitectureIsolationTest {

  @Test
  void eventBusIsAContractRatherThanAnApiImplementation() {
    assertTrue(EventBus.class.isInterface(), "EventBus implementation belongs outside api");
  }

  @Test
  void apiDoesNotExposeRuntimeRegistrationInternals() {
    assertThrows(
        NoSuchMethodException.class, () -> Bridge.class.getMethod("register", Bridge.class));
    assertThrows(NoSuchMethodException.class, () -> Bridge.class.getMethod("unregister"));
    assertThrows(
        NoSuchMethodException.class, () -> ConditionFactory.class.getMethod("conditionFactory"));
    assertTrue(
        Stream.of(Bridge.class.getClasses())
            .noneMatch(type -> type.getSimpleName().equals("Holder")),
        "Bridge must not publish its mutable holder");
  }

  @Test
  void apiDoesNotPublishImplementationClasses() throws IOException {
    Pattern publicImplementation =
        Pattern.compile("\\bpublic\\s+(?:final\\s+)?(?:class|record)\\s+\\w+Impl\\b");
    assertTrue(
        publicImplementation.matcher("public final class ExampleImpl {}").find(),
        "boundary pattern must detect public implementation classes");
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
      walk.filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  if (publicImplementation.matcher(Files.readString(path)).find()) {
                    offenders.add(path.toString());
                  }
                } catch (IOException exception) {
                  fail(exception);
                }
              });
    }
    assertTrue(offenders.isEmpty(), "Public implementation classes in api: " + offenders);
  }

  @Test
  void apiValueFactoriesDoNotExposeNestedImplementations() {
    for (Class<?> nestedType : Currency.class.getClasses()) {
      assertTrue(
          !nestedType.getSimpleName().endsWith("Impl"),
          () -> "Public nested implementation in api: " + nestedType.getName());
    }
  }

  @Test
  void apiDoesNotReferenceImplementationModules() throws IOException {
    Pattern forbiddenReference =
        Pattern.compile(
            "\\b(?:dev\\.mintychochip\\.common|org\\.bukkit|io\\.papermc|"
                + "org\\.spigotmc|com\\.destroystokyo|net\\.minecraft)\\.");
    assertTrue(
        forbiddenReference.matcher("dev.mintychochip.common.event.EventBusImpl").find(),
        "boundary pattern must detect implementation-module references");

    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
      walk.filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  if (forbiddenReference.matcher(Files.readString(path)).find()) {
                    offenders.add(path.toString());
                  }
                } catch (IOException exception) {
                  fail(exception);
                }
              });
    }
    assertTrue(offenders.isEmpty(), "Implementation dependencies in api: " + offenders);

    String buildScript = Files.readString(Path.of("build.gradle.kts"));
    assertTrue(
        !buildScript.contains("modularjobs-common") && !buildScript.contains("modularjobs-paper"),
        "api Gradle dependencies must not point at implementation modules");
  }

  @Test
  void apiAndCommonSourcesMustNotImportBukkitOrPaper() throws IOException {
    List<Path> roots =
        List.of(
            Path.of("src/main/java"),
            // common is sibling — also scan from repo via relative path when test runs from api
            // project
            Path.of("../modularjobs-common/src/main/java"));
    List<String> offenders = new ArrayList<>();
    for (Path root : roots) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        walk.filter(p -> p.toString().endsWith(".java"))
            .forEach(
                p -> {
                  try {
                    String text = Files.readString(p);
                    if (text.contains("import org.bukkit")
                        || text.contains("import io.papermc")
                        || text.contains("import org.spigotmc")
                        || text.contains("import de.flog99.mapgui")) {
                      offenders.add(p.toString());
                    }
                  } catch (IOException e) {
                    fail(e);
                  }
                });
      }
    }
    assertTrue(offenders.isEmpty(), "Bukkit/Paper imports in pure modules: " + offenders);
  }
}
