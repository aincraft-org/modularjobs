package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.JobTreeRecord;
import dev.mintychochip.math.ExpressionCurves;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link PlayerJobStateImpl} level-from-experience binary search + XP thresholds.
 */
class PlayerJobStateLevelTest {

  private static final UUID PLAYER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  private Job job;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    // Threshold for level N is N * 100 XP (curve evaluates level variable)
    job = job("miner", 10);
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void zeroExperienceIsLevelOne() {
    PlayerJobState state = new PlayerJobStateImpl(PLAYER_ID, job, BigDecimal.ZERO);
    assertEquals(1, state.level());
    assertEquals(0, BigDecimal.ZERO.compareTo(state.experience()));
  }

  @Test
  void experienceAtLevelThresholds() {
    // curve: evaluate(level) = level * 100 → reaching level 3 requires >= 300 XP
    assertEquals(1, levelAt(new BigDecimal("99")));
    assertEquals(1, levelAt(new BigDecimal("100"))); // meets level-1 threshold only
    assertEquals(2, levelAt(new BigDecimal("200")));
    assertEquals(3, levelAt(new BigDecimal("300")));
    assertEquals(5, levelAt(new BigDecimal("500")));
    assertEquals(10, levelAt(new BigDecimal("1000")));
  }

  @Test
  void experienceAboveMaxLevelCapsAtMaxLevel() {
    assertEquals(10, levelAt(new BigDecimal("99999")));
  }

  @Test
  void experienceForLevelUsesJobCurve() {
    PlayerJobState state = new PlayerJobStateImpl(PLAYER_ID, job, BigDecimal.ZERO);
    assertEquals(0, new BigDecimal("100.0").compareTo(state.experienceForLevel(1)));
    assertEquals(0, new BigDecimal("500.0").compareTo(state.experienceForLevel(5)));
    assertEquals(0, new BigDecimal("1000.0").compareTo(state.experienceForLevel(10)));
  }

  @Test
  void withExperienceReturnsNewInstanceWithRecalculatedLevel() {
    PlayerJobState base = new PlayerJobStateImpl(PLAYER_ID, job, BigDecimal.ZERO);
    PlayerJobState leveled = base.withExperience(new BigDecimal("400"));
    assertNotSame(base, leveled);
    assertEquals(1, base.level());
    assertEquals(4, leveled.level());
    assertEquals(0, new BigDecimal("400").compareTo(leveled.experience()));
  }

  @Test
  void withExperienceSameValueReturnsSameInstance() {
    BigDecimal xp = new BigDecimal("250");
    PlayerJobState base = new PlayerJobStateImpl(PLAYER_ID, job, xp);
    assertSame(base, base.withExperience(xp));
  }

  @Test
  void addExperienceIncrementsAndLevelsUp() {
    PlayerJobState base = new PlayerJobStateImpl(PLAYER_ID, job, new BigDecimal("150"));
    PlayerJobState next = base.addExperience(new BigDecimal("100"));
    assertEquals(0, new BigDecimal("250").compareTo(next.experience()));
    assertTrue(next.level() >= base.level());
    assertEquals(2, next.level());
  }

  @Test
  void maxLevelZeroOrNegativeDefaultsToLevelOne() {
    Job uncapped = job("free", 0);
    PlayerJobState state = new PlayerJobStateImpl(PLAYER_ID, uncapped, new BigDecimal("9999"));
    assertEquals(1, state.level());
  }

  private static @NotNull Job job(@NotNull String value, int maxLevel) {
    String rawKey = "modularjobs:" + value;
    JobKey jobKey = new JobKey(Key.key(rawKey));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
    JobNode root = new JobNodeImpl(jobKey, nodeKey, null, Component.text(value), Component.empty());
    JobRecord record = new JobRecord(rawKey, value, "", maxLevel, "level * 100", Map.of(), null);
    return new JobImpl(
        jobKey,
        root,
        Map.of(nodeKey, root),
        new JobTreeRecord(record, Map.of(rawKey, record)),
        maxLevel,
        ExpressionCurves.levelingCurve("level * 100"),
        Map.of());
  }

  private int levelAt(@NotNull BigDecimal experience) {
    return new PlayerJobStateImpl(PLAYER_ID, job, experience).level();
  }
}
