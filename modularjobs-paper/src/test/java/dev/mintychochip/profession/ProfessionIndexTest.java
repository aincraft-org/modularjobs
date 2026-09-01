package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessionIndexTest {

  private static final ProfessionDefinition MINING =
      new ProfessionDefinition("mining", "miner", ProfessionCategory.GATHERING, "Mining");
  private static final ProfessionDefinition COOKING =
      new ProfessionDefinition("cooking", "cooking", ProfessionCategory.CRAFTING, "Cooking");

  @Test
  void preservesOrderAndResolvesEverySupportedKeyShape() {
    ProfessionIndex index = new ProfessionIndex(List.of(COOKING, MINING));

    assertEquals(List.of(COOKING, MINING), index.tracks());
    assertEquals(MINING, index.resolve("mining").orElseThrow());
    assertEquals(MINING, index.resolve("miner").orElseThrow());
    assertEquals(MINING, index.resolve("  MiNeR  ").orElseThrow());
    assertEquals(MINING, index.resolve("modularjobs:miner").orElseThrow());
    assertTrue(index.resolve("builder").isEmpty());
    assertTrue(index.resolve(" ").isEmpty());
    assertTrue(index.resolve(null).isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> index.tracks().add(MINING));
  }

  @Test
  void rejectsDuplicateCanonicalIds() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new ProfessionIndex(
                    List.of(
                        MINING,
                        new ProfessionDefinition(
                            "mining", "deep_miner", ProfessionCategory.GATHERING, "Deep Mining"))));

    assertTrue(exception.getMessage().contains("mining"));
    assertTrue(exception.getMessage().contains("0"));
    assertTrue(exception.getMessage().contains("1"));
  }

  @Test
  void rejectsDuplicateStorageKeys() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new ProfessionIndex(
                    List.of(
                        MINING,
                        new ProfessionDefinition(
                            "excavation", "miner", ProfessionCategory.GATHERING, "Excavation"))));

    assertTrue(exception.getMessage().contains("miner"));
    assertTrue(exception.getMessage().contains("0"));
    assertTrue(exception.getMessage().contains("1"));
  }

  @Test
  void rejectsCrossKindAmbiguity() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new ProfessionIndex(
                    List.of(
                        MINING,
                        new ProfessionDefinition(
                            "miner", "excavator", ProfessionCategory.GATHERING, "Excavation"))));

    assertTrue(exception.getMessage().contains("miner"));
    assertTrue(exception.getMessage().contains("0"));
    assertTrue(exception.getMessage().contains("1"));
  }
}
