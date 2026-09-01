package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.service.ProfessionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfessionServiceImplTest {

  private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final ProfessionDefinition MINING =
      new ProfessionDefinition("mining", "miner", ProfessionCategory.GATHERING, "Mining");

  @Test
  void exposesInjectedDefinitionsAndUsesTheirStorageKeys() {
    StubJobService jobs = StubJobService.withJobs("miner");
    ProfessionService service =
        new ProfessionServiceImpl(jobs, new ProfessionIndex(List.of(MINING)));

    assertEquals(List.of(MINING), service.tracks());
    assertEquals(MINING, service.resolve("modularjobs:miner").orElseThrow());
    assertTrue(service.level(PLAYER_ID, "mining").isEmpty());
    assertEquals("miner", jobs.lastProgressionKey);
    assertTrue(service.ensureTrack(PLAYER_ID, "miner"));
    assertEquals("miner", jobs.lastJoinedKey);
  }

  @Test
  void unknownProfessionDoesNotTouchJobService() {
    StubJobService jobs = StubJobService.withJobs("miner");
    ProfessionService service =
        new ProfessionServiceImpl(jobs, new ProfessionIndex(List.of(MINING)));

    assertTrue(service.level(PLAYER_ID, "builder").isEmpty());
    assertFalse(service.ensureTrack(PLAYER_ID, "builder"));
    assertNull(jobs.lastProgressionKey);
    assertNull(jobs.lastJoinedKey);
  }
}
