package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link PlayerJobStateService} archive/restore migration against in-memory repos.
 */
class PlayerJobStateServiceArchiveTest {

  private InMemoryStateRepository live;
  private InMemoryStateRepository archive;
  private PlayerJobStateService service;
  private JobRecord job;
  private PlayerJobStateRecord state;

  @BeforeEach
  void setUp() {
    live = new InMemoryStateRepository();
    archive = new InMemoryStateRepository();
    service = new PlayerJobStateService(live, archive);
    job =
        new JobRecord(
            "modularjobs:miner",
            "Miner",
            "Mines blocks",
            100,
            "level * 100",
            Map.of("currency", "base"),
            null);
    state =
        new PlayerJobStateRecord("player-1", job.jobKey(), job.jobKey(), new BigDecimal("1500.50"));
  }

  @Test
  void saveAndLoadFromLive() {
    assertTrue(service.save(state));
    PlayerJobStateRecord loaded = service.load("player-1", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals(new BigDecimal("1500.50"), loaded.experience());
    assertEquals("modularjobs:miner", loaded.jobKey());
  }

  @Test
  void archiveMovesRecordFromLiveToArchive() {
    assertTrue(service.save(state));
    assertTrue(service.archive("player-1", "modularjobs:miner"));

    assertNull(service.load("player-1", "modularjobs:miner"), "must leave live store");
    assertNull(live.load("player-1", "modularjobs:miner"));

    List<PlayerJobStateRecord> archived = service.loadAllArchivedForPlayer("player-1", 10);
    assertEquals(1, archived.size());
    assertEquals(new BigDecimal("1500.50"), archived.get(0).experience());
    assertNotNull(archive.load("player-1", "modularjobs:miner"));
  }

  @Test
  void restoreMovesRecordFromArchiveToLive() {
    assertTrue(service.save(state));
    assertTrue(service.archive("player-1", "modularjobs:miner"));
    assertTrue(service.restore("player-1", "modularjobs:miner"));

    PlayerJobStateRecord restored = service.load("player-1", "modularjobs:miner");
    assertNotNull(restored);
    assertEquals(new BigDecimal("1500.50"), restored.experience());
    assertTrue(service.loadAllArchivedForPlayer("player-1", 10).isEmpty());
    assertNull(archive.load("player-1", "modularjobs:miner"));
  }

  @Test
  void archiveMissingRecordReturnsFalse() {
    assertFalse(service.archive("missing", "modularjobs:miner"));
  }

  @Test
  void restoreMissingRecordReturnsFalse() {
    assertFalse(service.restore("missing", "modularjobs:miner"));
  }

  @Test
  void deleteRemovesLiveState() {
    assertTrue(service.save(state));
    assertTrue(service.delete("player-1", "modularjobs:miner"));
    assertNull(service.load("player-1", "modularjobs:miner"));
  }

  @Test
  void loadAllForPlayerRespectsLimit() {
    JobRecord fisher =
        new JobRecord("modularjobs:fisherman", "Fisher", "Fish", 50, "level*10", Map.of(), null);
    service.save(state);
    service.save(
        new PlayerJobStateRecord("player-1", fisher.jobKey(), fisher.jobKey(), BigDecimal.TEN));

    List<PlayerJobStateRecord> limited = service.loadAllForPlayer("player-1", 1);
    assertEquals(1, limited.size());

    List<PlayerJobStateRecord> all = service.loadAllForPlayer("player-1", 10);
    assertEquals(2, all.size());
  }

  /** In-memory fake for collaborator only — SUT is PlayerJobStateService. */
  private static final class InMemoryStateRepository implements PlayerJobStateRepository {

    private final Map<String, PlayerJobStateRecord> store = new ConcurrentHashMap<>();

    private static @NotNull String key(@NotNull String playerId, @NotNull String jobKey) {
      return playerId + "|" + jobKey;
    }

    @Override
    public boolean save(@NotNull PlayerJobStateRecord record) {
      store.put(key(record.playerId(), record.jobKey()), record);
      return true;
    }

    @Override
    public @Nullable PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey) {
      return store.get(key(playerId, jobKey));
    }

    @Override
    public @NotNull List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit) {
      return store.values().stream().filter(r -> r.jobKey().equals(jobKey)).limit(limit).toList();
    }

    @Override
    public @NotNull List<PlayerJobStateRecord> loadAllForPlayer(
        @NotNull String playerId, int limit) {
      List<PlayerJobStateRecord> result = new ArrayList<>();
      for (PlayerJobStateRecord record : store.values()) {
        if (record.playerId().equals(playerId)) {
          result.add(record);
          if (result.size() >= limit) {
            break;
          }
        }
      }
      return result;
    }

    @Override
    public boolean delete(@NotNull String playerId, @NotNull String jobKey) {
      return store.remove(key(playerId, jobKey)) != null;
    }
  }
}
