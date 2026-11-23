package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class CiTemplateHookupTest {
  private static final String RELEASE_VERSION = "26.8.17.99";

  @Test
  void consumerWorkflowBuildsAndUploadsApiArtifact() throws IOException {
    Path workflow = Path.of(requiredProperty("ci.workflow"));
    String text = Files.readString(workflow);
    assertTrue(
        text.contains("name: API build + publish artifact"),
        "missing inline api-build job in " + workflow);
    assertTrue(
        text.contains(":modularjobs-api:test :modularjobs-api:build"),
        "missing prefixed api gradle tasks in " + workflow);
    assertFalse(
        text.contains(":api:test :api:build"),
        "unprefixed :api: task paths must not remain in " + workflow);
    assertTrue(
        text.contains(":modularjobs-paper:shadowJar"),
        "missing prefixed paper shadowJar in " + workflow);
    assertFalse(
        text.contains("./gradlew :paper:shadowJar"),
        "unprefixed :paper:shadowJar must not remain in " + workflow);
    assertTrue(
        text.contains("modularjobs-api/build/libs/*.jar"),
        "missing api jar upload glob in " + workflow);
    assertFalse(
        text.contains("aincraft-org/ci-template/.github/workflows/paper.yml@"),
        "reusable workflow must not be referenced from "
            + workflow
            + " (aincraft-org/ci-template is private and unresolvable from a public repo)");
  }

  @Test
  void gradleIncludesPrefixedJavaModulesAndNotWeb() throws IOException {
    Path root = Path.of(requiredProperty("project.root"));
    String settings = Files.readString(root.resolve("settings.gradle.kts"));
    assertTrue(settings.contains("\"modularjobs-api\""), settings);
    assertTrue(settings.contains("\"modularjobs-common\""), settings);
    assertTrue(settings.contains("\"modularjobs-paper\""), settings);
    assertFalse(
        settings.contains("include(\"api\", \"common\", \"paper\")"),
        "unprefixed include() must not remain");
    assertFalse(settings.contains("include(\"web\")"), "web must not be a Gradle included project");
    String hooks = Files.readString(root.resolve(".githooks/pre-commit"));
    assertTrue(
        hooks.contains(":modularjobs-api:test :modularjobs-common:test :modularjobs-paper:test"),
        "pre-commit must run prefixed module tests");
    assertFalse(
        hooks.contains(":api:test :common:test :paper:test"),
        "unprefixed module test paths must not remain in pre-commit");
  }

  @Test
  void cleanCloneBuildsWithoutSiblingCheckouts() throws IOException {
    Path root = Path.of(requiredProperty("project.root"));
    String settings = Files.readString(root.resolve("settings.gradle.kts"));
    assertFalse(settings.contains("includeBuild("), "no included sibling builds");
    assertFalse(settings.contains("\"../"), "no sibling Maven repositories");
    assertFalse(settings.contains("mavenLocal()"), "no machine-local Maven repository");
    assertFalse(settings.contains("maven.pkg.github.com"), "no token-gated repositories");
    for (String workflow :
        new String[] {
          requiredProperty("ci.workflow"), root.resolve(".github/workflows/nightly.yml").toString()
        }) {
      String text = Files.readString(Path.of(workflow));
      assertFalse(text.contains("$GITHUB_WORKSPACE/../"), workflow + " must not link siblings");
    }
  }

  @Test
  void ciUsesPinnedActionMajorsAndCleanCheck() throws IOException {
    String text = Files.readString(Path.of(requiredProperty("ci.workflow")));
    assertTrue(text.contains("actions/checkout@v7"), "checkout must be @v7");
    assertTrue(text.contains("actions/setup-java@v5"), "setup-java must be @v5");
    assertTrue(text.contains("gradle/actions/setup-gradle@v6"), "setup-gradle must be @v6");
    assertTrue(text.contains("./gradlew clean check"), "java gate must be clean check");
    assertFalse(
        text.contains("java-version: \"21\""),
        "CI must not install JDK 21 after the Java 25-only cutover");
  }

  @Test
  void nightlyIsScheduleOrManualAndPublishesShadowJar() throws IOException {
    Path nightly =
        Path.of(requiredProperty("project.root")).resolve(".github/workflows/nightly.yml");
    assertTrue(Files.isRegularFile(nightly), "missing " + nightly);
    String text = Files.readString(nightly);
    assertTrue(text.contains("cron: '0 4 * * *'"), "nightly must schedule at 04:00 UTC");
    assertTrue(text.contains("workflow_dispatch"), "nightly must be manually dispatchable");
    assertTrue(text.contains("gh release create nightly"), "must replace rolling nightly tag");
    assertTrue(
        text.contains(":modularjobs-paper:shadowJar"),
        "nightly must build the prefixed paper shadow jar");
    assertFalse(
        text.contains("./gradlew :paper:shadowJar"),
        "unprefixed :paper:shadowJar must not remain in nightly.yml");
    String shadowJarGlob = "modularjobs-paper/build/libs/*-all.jar";
    assertTrue(text.contains(shadowJarGlob), "must publish paper shadow jar");
    assertTrue(isScheduleOrManualOnly(text), "nightly must not run on push/PR/tag");
  }

  @Test
  void leftoverLocalJobsDoNotPublishOnPushPrOrTag() throws IOException {
    Path dir = Path.of(requiredProperty("project.root")).resolve(".github/workflows");
    try (var stream = Files.list(dir)) {
      for (Path file : stream.filter(path -> path.toString().endsWith(".yml")).toList()) {
        String text = Files.readString(file);
        if (!publishesPackagesOrRelease(text)) {
          continue;
        }
        assertTrue(
            isScheduleOrManualOnly(text), "leftover Packages/Release on push/PR/tag in " + file);
      }
    }
  }

  @Test
  void releaseVersionBecomesPublishedPomVersion() throws Exception {
    Path root = Path.of(requiredProperty("project.root"));
    ProcessBuilder builder =
        new ProcessBuilder(
            root.resolve("gradlew").toAbsolutePath().toString(),
            "--no-daemon",
            "-q",
            ":modularjobs-api:generatePomFileForMavenPublication",
            "-PreleaseVersion=" + RELEASE_VERSION);
    builder.directory(root.toFile());
    builder.redirectErrorStream(true);
    Process process = builder.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(process.waitFor(5, TimeUnit.MINUTES), "gradlew timed out");
    assertEquals(0, process.exitValue(), output);
    String pom = Files.readString(Path.of(requiredProperty("ci.pom")));
    assertTrue(pom.contains("<version>" + RELEASE_VERSION + "</version>"), pom);
    assertTrue(pom.contains("<artifactId>modularjobs-api</artifactId>"), pom);
    assertFalse(pom.contains("modularjobs-modularjobs-api"), pom);
    String build = Files.readString(root.resolve("build.gradle.kts"));
    assertTrue(build.contains("https://maven.pkg.github.com/aincraft-org/modularjobs"), build);
    assertTrue(build.contains("GITHUB_ACTOR"), build);
    assertTrue(build.contains("GITHUB_TOKEN"), build);
  }

  @Contract(pure = true)
  private static boolean publishesPackagesOrRelease(@NotNull String text) {
    return text.contains("ToGitHubPackages")
        || text.contains("softprops/action-gh-release")
        || text.contains("gh release create");
  }

  @Contract(pure = true)
  private static boolean isScheduleOrManualOnly(@NotNull String text) {
    return !text.contains("\n  push:")
        && !text.contains("\n  pull_request:")
        && !text.contains("\n  tags:");
  }

  private static @NotNull String requiredProperty(@NotNull String name) {
    String value = System.getProperty(name);
    assertTrue(value != null && !value.isBlank(), "missing system property " + name);
    return value;
  }
}
