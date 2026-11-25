package dev.mintychochip.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgressionLimitsConfigTest {

  @Test
  void parsesSlotGroupsAndAutoJoin() {
    ProgressionLimitsConfig config =
        ProgressionLimitsConfig.fromMap(
            Map.of(
                "slot-groups",
                Map.of(
                    "primary", Map.of("slots", 2, "jobs", List.of("Miner", "logger")),
                    "secondary", Map.of("slots", 1, "jobs", List.of("fisher"))),
                "auto-join-jobs",
                List.of("miner", "farmer"),
                "world-join-restriction",
                true));
    assertTrue(config.hasSlotGroups());
    assertEquals(2, config.groupOf("MINER").slots());
    assertEquals(Set.of("miner", "logger"), config.groupOf("logger").jobs());
    assertEquals("secondary", config.groupOf("fisher").name());
    assertNull(config.groupOf("hunter"));
    assertEquals(List.of("miner", "farmer"), config.autoJoinJobs());
    assertTrue(config.worldJoinRestriction());
  }

  @Test
  void defaultsAreUnlimited() {
    ProgressionLimitsConfig config = ProgressionLimitsConfig.fromMap(Map.of());
    assertFalse(config.hasSlotGroups());
    assertTrue(config.autoJoinJobs().isEmpty());
    assertTrue(config.worldJoinRestriction());
  }

  @Test
  void normalizesAutoJoinNames() {
    ProgressionLimitsConfig config =
        ProgressionLimitsConfig.fromMap(Map.of("auto-join-jobs", List.of("Miner", "  ", "FARMER")));
    assertEquals(List.of("miner", "farmer"), config.autoJoinJobs());
  }

  @Test
  void removedMaxJobsKeyFailsLoudly() {
    // A silently ignored max-jobs would lift the server's job limit.
    assertThrows(
        IllegalArgumentException.class,
        () -> ProgressionLimitsConfig.fromMap(Map.of("max-jobs", 3)));
  }

  @Test
  void jobInTwoGroupsIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ProgressionLimitsConfig.fromMap(
                Map.of(
                    "slot-groups",
                    Map.of(
                        "primary", Map.of("slots", 2, "jobs", List.of("miner")),
                        "secondary", Map.of("slots", 1, "jobs", List.of("miner"))))));
  }

  @Test
  void groupWithoutPositiveSlotsIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ProgressionLimitsConfig.fromMap(
                Map.of("slot-groups", Map.of("primary", Map.of("slots", 0)))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ProgressionLimitsConfig.fromMap(
                Map.of("slot-groups", Map.of("primary", Map.of("jobs", List.of("miner"))))));
  }
}
