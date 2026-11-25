package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.BridgeFixture;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.config.ProgressionLimitsConfig.SlotGroup;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import dev.mintychochip.event.JobSwapEvent;
import dev.mintychochip.event.JobSwapRequestEvent;
import dev.mintychochip.registry.SimpleRegistryImpl;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.aincraft.api.event.EventBus;
import org.aincraft.event.EventBuses;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Drives {@link JobServiceImpl} slot groups, hold, swap and the swap events end to end. */
class JobSlotSwapTest {

  private ServerMock server;
  private Plugin plugin;
  private PlayerMock player;
  private String id;
  private JobServiceImpl jobs;
  private PlayerJobStateService states;
  private final List<JobSwapRequestEvent> requests = new ArrayList<>();
  private final List<JobSwapEvent> swaps = new ArrayList<>();
  private final AtomicBoolean refuse = new AtomicBoolean();

  @BeforeEach
  void setUp() {
    server = MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
    EventBus bus = EventBuses.create();
    bus.subscribe(
        JobSwapRequestEvent.class,
        event -> {
          requests.add(event);
          event.setCancelled(refuse.get());
        });
    bus.subscribe(JobSwapEvent.class, swaps::add);
    BridgeFixture.install(bus);
    player = server.addPlayer();
    player.setOp(true); // jobs.join.<job>
    id = player.getUniqueId().toString();

    Map<String, JobRecord> records = new LinkedHashMap<>();
    for (String name : List.of("miner", "logger", "harvester", "fisher")) {
      records.put(
          "modularjobs:" + name,
          new JobRecord("modularjobs:" + name, name, null, 100, "level * 100", Map.of(), null));
    }
    states = new PlayerJobStateService(new MemoryStates(), new MemoryStates());
    ProgressionLimitsConfig limits =
        new ProgressionLimitsConfig(
            List.of(
                new SlotGroup("primary", 2, Set.of("miner", "logger", "harvester")),
                new SlotGroup("secondary", 1, Set.of("fisher"))),
            List.of(),
            false);
    jobs =
        new JobServiceImpl(
            new SimpleRegistryImpl<>(),
            new SimpleRegistryImpl<>(),
            null, // tasks are not read by join/leave/swap
            null,
            new MemoryJobRepositoryImpl(records),
            states,
            new JoinGate(limits, Set.of()),
            plugin);
  }

  @AfterEach
  void tearDown() {
    BridgeFixture.uninstall();
    MockBukkitSupport.unmockServer();
  }

  @Test
  void slotGroupsLimitJoinsPerGroup() {
    assertTrue(jobs.joinJob(id, "modularjobs:miner"));
    assertTrue(jobs.joinJob(id, "modularjobs:logger"));
    assertTrue(jobs.joinJob(id, "modularjobs:fisher"), "the secondary slot is separate");
    assertFalse(jobs.joinJob(id, "modularjobs:harvester"), "both primary slots are full");
    assertTrue(requests.isEmpty(), "first joins are free and fire no swap request");
  }

  @Test
  void swapPutsTheOldJobOnHoldAndBringsItBackWithItsExperience() {
    jobs.joinJob(id, "modularjobs:miner");
    jobs.joinJob(id, "modularjobs:logger");
    PlayerJobState logger = jobs.getPlayerJobState(id, "modularjobs:logger");
    assertNotNull(logger);
    BigDecimal earned = new BigDecimal("950");
    states.save(new PlayerJobStateRecord(id, "modularjobs:logger", "modularjobs:logger", earned));

    assertTrue(jobs.swapJob(id, "modularjobs:logger", "modularjobs:harvester"));
    assertNull(jobs.getPlayerJobState(id, "modularjobs:logger"));
    assertNotNull(states.loadArchived(id, "modularjobs:logger"), "logger is on hold");
    assertNotNull(jobs.getPlayerJobState(id, "modularjobs:harvester"));
    assertEquals(1, requests.size());
    assertEquals("logger", requests.get(0).getFrom().key().value());
    assertFalse(requests.get(0).isReactivation(), "harvester is new");
    assertEquals(1, swaps.size());

    assertTrue(jobs.swapJob(id, "modularjobs:harvester", "modularjobs:logger"));
    assertTrue(requests.get(1).isReactivation());
    assertTrue(swaps.get(1).isReactivation());
    assertEquals(
        0, earned.compareTo(jobs.getPlayerJobState(id, "modularjobs:logger").experience()));
    assertNotNull(states.loadArchived(id, "modularjobs:harvester"), "harvester is on hold");
  }

  @Test
  void refusedSwapChangesNothing() {
    jobs.joinJob(id, "modularjobs:miner");
    jobs.joinJob(id, "modularjobs:logger");
    refuse.set(true);

    assertFalse(jobs.swapJob(id, "modularjobs:logger", "modularjobs:harvester"));
    assertNotNull(jobs.getPlayerJobState(id, "modularjobs:logger"));
    assertNull(jobs.getPlayerJobState(id, "modularjobs:harvester"));
    assertNull(states.loadArchived(id, "modularjobs:logger"));
    assertTrue(swaps.isEmpty());
  }

  @Test
  void swapIntoFullOtherGroupIsRefusedBeforeAnyEvent() {
    jobs.joinJob(id, "modularjobs:miner");
    jobs.joinJob(id, "modularjobs:logger");
    jobs.joinJob(id, "modularjobs:fisher");

    assertFalse(jobs.swapJob(id, "modularjobs:fisher", "modularjobs:harvester"));
    assertNotNull(jobs.getPlayerJobState(id, "modularjobs:fisher"));
    assertTrue(requests.isEmpty());
  }

  @Test
  void rejoiningHeldJobIsReactivationListenersMayRefuse() {
    jobs.joinJob(id, "modularjobs:miner");
    assertTrue(jobs.leaveJob(id, "modularjobs:miner"));
    refuse.set(true);

    assertFalse(jobs.joinJob(id, "modularjobs:miner"));
    assertNull(requests.get(0).getFrom(), "a free slot is filled");
    assertTrue(requests.get(0).isReactivation());
    assertNotNull(states.loadArchived(id, "modularjobs:miner"), "still on hold");

    refuse.set(false);
    assertTrue(jobs.joinJob(id, "modularjobs:miner"));
    assertEquals(1, swaps.size());
    assertNull(states.loadArchived(id, "modularjobs:miner"));
  }

  /** In-memory store standing in for the live and archive tables. */
  private static final class MemoryStates implements PlayerJobStateRepository {

    private final Map<String, PlayerJobStateRecord> store = new ConcurrentHashMap<>();

    @Override
    public boolean save(@NotNull PlayerJobStateRecord record) {
      store.put(record.playerId() + "|" + record.jobKey(), record);
      return true;
    }

    @Override
    public @Nullable PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey) {
      return store.get(playerId + "|" + jobKey);
    }

    @Override
    public @NotNull List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit) {
      return store.values().stream().filter(r -> r.jobKey().equals(jobKey)).limit(limit).toList();
    }

    @Override
    public @NotNull List<PlayerJobStateRecord> loadAllForPlayer(
        @NotNull String playerId, int limit) {
      return store.values().stream()
          .filter(r -> r.playerId().equals(playerId))
          .limit(limit)
          .toList();
    }

    @Override
    public boolean delete(@NotNull String playerId, @NotNull String jobKey) {
      return store.remove(playerId + "|" + jobKey) != null;
    }
  }
}
