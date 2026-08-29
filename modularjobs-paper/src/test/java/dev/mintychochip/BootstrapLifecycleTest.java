package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.editor.EditorConfig;
import dev.mintychochip.gui.PaperSurfaces;
import dev.mintychochip.gui.PaperUiHost;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.DatabaseType;
import dev.mintychochip.repository.PluginResources;
import dev.mintychochip.test.MockBukkitSupport;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Structural proof of disable lifecycle and profession service gating in shipped sources. */
class BootstrapLifecycleTest {

  @Test
  void onDisableUnregistersServicesAndPapiAndDoesNotBusySpin() throws Exception {
    Path bootstrap = locate("ModularJobsBootstrap.java");
    String text = Files.readString(bootstrap, StandardCharsets.UTF_8);
    assertTrue(text.contains("Bridge.register"), "must register static Bridge holder on enable");
    assertTrue(text.contains("Bridge.unregister"), "must clear static Bridge holder on disable");
    assertTrue(text.contains("PluginProvider.set"), "must set/clear paper PluginProvider");
    assertTrue(text.contains("unregisterAll"), "must unregister Bukkit services on disable");
    assertTrue(text.contains("unregister()"), "must unregister PlaceholderAPI expansion");
    assertTrue(text.contains("ctx.shutdown()"), "must flush/close via PluginContext.shutdown");
    assertFalse(
        text.contains("onSpinWait"),
        "bootstrap must not busy-spin; flush wait lives in write-back with sleep/timeout");
  }

  @Test
  void professionServicesGatedByConfig() throws Exception {
    Path bootstrap = locate("ModularJobsBootstrap.java");
    String text = Files.readString(bootstrap, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("profession-apis.register-bukkit-services"),
        "profession Bukkit services must be feature-flagged");
  }

  @Test
  void pluginContextWiresLocalPreferencesService() throws Exception {
    Path context = locate("PluginContext.java");
    String text = Files.readString(context, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("new PreferencesServiceImpl(plugin)"),
        "PluginContext must construct the always-available local preference service");
    assertTrue(
        text.contains("PreferencesIntegration.wire(plugin)"),
        "PluginContext must wire optional external XP bar color preference");
    assertTrue(
        text.contains("resources.onFlush(preferencesWiring.onDisable())"),
        "PluginContext must unregister external preference on disable");
    assertFalse(
        text.contains("ExternalBackedPreferencesService"),
        "PluginContext must not depend on the removed external facade");
  }

  @Test
  void pluginContextRetainsNativeUiAndClosesItBeforeResources() throws Exception {
    String context = Files.readString(locate("PluginContext.java"), StandardCharsets.UTF_8);
    String compact = context.replaceAll("\\s+", " ");

    assertTrue(
        context.contains("public final PaperUiHost paperUiHost;"),
        "PluginContext must retain the native inventory host");
    assertTrue(
        context.contains("public final PaperSurfaces paperSurfaces;"),
        "PluginContext must retain the native surface manager");
    assertTrue(
        context.contains("this.paperUiHost = paperUiHost;")
            && context.contains("this.paperSurfaces = paperSurfaces;"),
        "PluginContext must own the same native managers it composes");
    assertTrue(
        compact.contains("final PaperUiHost paperUiHost = new PaperUiHost()")
            && compact.contains("final PaperSurfaces paperSurfaces = new PaperSurfaces()"),
        "PluginContext must create native UI managers");

    assertTrue(
        compact.contains("new JobBrowseGui(paperUiHost")
            && compact.contains("new JobInfoGui(paperUiHost")
            && compact.contains("new StatsGui(paperUiHost")
            && compact.contains("new UpgradeTreeGui(paperUiHost"),
        "browse, info, stats, and upgrade presenters must share the native host");
    assertTrue(
        compact.contains("new BrowseCommand(jobBrowseGui)")
            && compact.contains(
                "new InfoCommand(domain.jobService, domain.jobResolver, preferencesService, jobInfoGui)")
            && compact.contains("new StatsCommand(domain.jobService, statsGui)")
            && compact.contains(
                "new UpgradesCommand(upgradeService, domain.jobResolver, upgradeTreeGui)")
            && compact.contains(
                "new TreeEditorCommand( upgradeService, domain.jobResolver, treeEditorGui"),
        "migrated commands must receive their native presenters");
    String top = Files.readString(locate("commands/TopCommand.java"), StandardCharsets.UTF_8);
    String topCompact = top.replaceAll("\\s+", " ");
    String scoreboard =
        Files.readString(locate("commands/TextScoreboard.java"), StandardCharsets.UTF_8);
    assertTrue(
        top.contains("private final PaperSurfaces surfaces;")
            && topCompact.contains("TextScoreboard.create( surfaces,")
            && scoreboard.contains("private final PaperSurfaces surfaces;")
            && scoreboard.contains("surfaces.showScoreboard"),
        "TopCommand and TextScoreboard must use the native surface manager");

    String payable =
        Files.readString(locate("payable/PayableWiring.java"), StandardCharsets.UTF_8);
    String payableCompact = payable.replaceAll("\\s+", " ");
    assertTrue(
        compact.contains(
                "PayableWiring.create( plugin, domain.jobService, payableTypeRegistry, paperSurfaces")
            && payable.contains("public final List<Listener> listeners;")
            && payableCompact.contains("new ExperienceBarControllerImpl(plugin, surfaces)")
            && payableCompact.contains("List<Listener> listeners = List.of(controller)"),
        "PluginContext must pass its native surfaces to PayableWiring and compose its XP listener");

    String experience =
        Files.readString(
            locate("payable/ExperienceBarControllerImpl.java"), StandardCharsets.UTF_8);
    assertTrue(
        experience.contains("private final PaperSurfaces surfaces;")
            && experience.contains("surfaces.showBossBar"),
        "ExperienceBarControllerImpl must render through native surfaces");
    assertFalse(
        experience.contains("registerEvents"),
        "XP listener registration must be owned by PluginContext");

    assertTrue(
        compact.contains("listenerList.add(paperUiHost)")
            && compact.contains("listenerList.addAll(payables.listeners)")
            && compact.contains("listenerList.addAll(payment.listeners)"),
        "native host and payable listeners must be composed before payment listeners");

    assertEquals(
        1,
        countOccurrences(context, "listenerList.add(paperUiHost)"),
        "native host must be registered exactly once");

    int hostListener = context.indexOf("listenerList.add(paperUiHost)");
    int payableListeners = context.indexOf("listenerList.addAll(payables.listeners)");
    int paymentListeners = context.indexOf("listenerList.addAll(payment.listeners)");
    assertTrue(
        hostListener >= 0
            && payableListeners > hostListener
            && paymentListeners > payableListeners,
        "native host, payable, and payment listeners must retain registration order");

    int hostClose = context.indexOf("paperUiHost.closeAll();");
    int surfacesClose = context.indexOf("paperSurfaces.closeAll();");
    int resourcesClose = context.indexOf("resources.shutdown();");
    assertEquals(1, countOccurrences(context, "paperUiHost.closeAll();"));
    assertEquals(1, countOccurrences(context, "paperSurfaces.closeAll();"));
    assertTrue(
        hostClose >= 0
            && surfacesClose >= 0
            && resourcesClose >= 0
            && hostClose < resourcesClose
            && surfacesClose < resourcesClose,
        "native UI must close before database resources");
  }


  @Test
  void pluginContextShutdownClosesNativeStateBeforeResourcesAndOnlyOnce() throws Exception {
    MockBukkitSupport.mockServer();
    try {
      PlayerMock player = MockBukkitSupport.mockServer().addPlayer("lifecycle");
      PaperUiHost host = new PaperUiHost();
      PaperSurfaces surfaces = new PaperSurfaces();
      PluginResources resources = new PluginResources();
      List<String> events = new ArrayList<>();
      RecordingConnectionSource source = new RecordingConnectionSource(events);
      resources.track(source);
      resources.onFlush(
          () -> {
            events.add("resource-flush");
            assertTrue(mapField(host, "sessions").isEmpty(), "host sessions must close before flush");
            assertTrue(
                mapField(surfaces, "activeScoreboards").isEmpty(),
                "scoreboards must close before flush");
            assertTrue(
                mapField(surfaces, "bossBars").isEmpty(), "boss bars must close before flush");
          });

      AtomicInteger closeCallbacks = new AtomicInteger();
      host.open(
          player,
          new PaperUiHost.ScreenView(
              "lifecycle",
              1,
              Component.text("Lifecycle"),
              Map.of(),
              Map.of(),
              ignored -> {
                closeCallbacks.incrementAndGet();
                events.add("ui-close");
                assertTrue(mapField(host, "sessions").isEmpty(), "host session must be removed first");
              }));
      surfaces.showScoreboard(player.getUniqueId(), "Lifecycle", List.of("line"));
      surfaces.showBossBar(
          player.getUniqueId(), "lifecycle", "Lifecycle", 0.5, Color.BLUE);

      PluginContext context =
          new PluginContext(
              null, source, resources, host, surfaces, null, Set.of(), Set.of(), null);

      context.shutdown();
      context.shutdown();

      assertEquals(List.of("ui-close", "resource-flush", "resource-shutdown"), events);
      assertEquals(1, closeCallbacks.get(), "host close callback must run once");
      assertEquals(1, source.shutdownCalls, "resource shutdown must run once");
      assertTrue(source.closed, "resource source must be closed");
      assertTrue(mapField(host, "sessions").isEmpty());
      assertTrue(mapField(surfaces, "activeScoreboards").isEmpty());
      assertTrue(mapField(surfaces, "bossBars").isEmpty());
    } finally {
      MockBukkitSupport.unmockServer();
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<?, ?> mapField(Object target, String name) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return (Map<?, ?>) field.get(target);
    } catch (ReflectiveOperationException failure) {
      throw new LinkageError("Cannot inspect " + name, failure);
    }
  }

  private static final class RecordingConnectionSource implements ConnectionSource {
    private final List<String> events;
    private boolean closed;
    private int shutdownCalls;

    private RecordingConnectionSource(List<String> events) {
      this.events = events;
    }

    @Override
    public void shutdown() throws SQLException {
      shutdownCalls++;
      events.add("resource-shutdown");
      closed = true;
    }

    @Override
    public DatabaseType getType() {
      return DatabaseType.MYSQL;
    }

    @Override
    public boolean isClosed() {
      return closed;
    }

    @Override
    public Connection getConnection() throws SQLException {
      throw new UnsupportedOperationException("test source has no connections");
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }

  private static int countOccurrences(String text, String needle) {
    int count = 0;
    int offset = 0;
    while ((offset = text.indexOf(needle, offset)) >= 0) {
      count++;
      offset += needle.length();
    }
    return count;
  }

  @Test
  void disabledEditorDefaultsGateRestClientConstruction() throws Exception {
    EditorConfig defaults = EditorConfig.defaults();
    assertFalse(defaults.enabled());
    assertEquals("", defaults.sessionApiUrl());
    assertEquals("", defaults.webEditorUrl());

    String text = Files.readString(locate("PluginContext.java"), StandardCharsets.UTF_8);
    int commandGate = text.indexOf("if (editorConfig.enabled())");
    int clientConstruction = text.indexOf("new RestSessionClient(editorConfig, gson)");
    assertTrue(commandGate >= 0, "editor commands must be gated by enabled configuration");
    assertTrue(
        clientConstruction > commandGate,
        "disabled editor defaults must not construct RestSessionClient before the gate");
  }

  @Test
  void progressionWriteBackFlushPendingUsesSleepNotSpinWait() throws Exception {
    Path writeBack = locate("domain/WriteBackJobProgressionRepositoryImpl.java");
    String text = Files.readString(writeBack, StandardCharsets.UTF_8);
    assertTrue(text.contains("Thread.sleep"), "flushPending must sleep while waiting for lock");
    assertFalse(text.contains("onSpinWait"), "flushPending must not busy-spin");
    assertTrue(
        text.contains("preferHigherExperience") || text.contains("merge("),
        "flush failure re-queue must merge XP safely");
    assertTrue(text.contains("key.jobKey()"), "loadAllForJob must compare job key");
  }

  @Test
  void timedBoostWriteBackFlushPendingUsesSleepNotSpinWait() throws Exception {
    Path writeBack = locate("repository/WriteBackRepositoryImpl.java");
    String text = Files.readString(writeBack, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("void flushPending()"),
        "WriteBackRepositoryImpl must expose flushPending for disable path");
    assertTrue(
        text.contains("Thread.sleep"),
        "WriteBackRepositoryImpl.flushPending must sleep while waiting for lock");
    assertFalse(
        text.contains("onSpinWait"),
        "WriteBackRepositoryImpl.flushPending must not busy-spin (timed-boost disable hang)");
    assertTrue(
        text.contains("FLUSH_LOCK_WAIT_MS") || text.contains("Timed out waiting"),
        "WriteBackRepositoryImpl.flushPending must timeout instead of hang forever");
  }

  private static Path locate(String relativeUnderAincraft) {
    Path root = Path.of("").toAbsolutePath();
    Path candidate =
        root.resolve("modularjobs-paper/src/main/java/dev/mintychochip/" + relativeUnderAincraft);
    if (Files.isRegularFile(candidate)) {
      return candidate;
    }
    candidate = root.resolve("src/main/java/dev/mintychochip/" + relativeUnderAincraft);
    if (Files.isRegularFile(candidate)) {
      return candidate;
    }
    throw new IllegalStateException("Cannot find " + relativeUnderAincraft + " from " + root);
  }
}
