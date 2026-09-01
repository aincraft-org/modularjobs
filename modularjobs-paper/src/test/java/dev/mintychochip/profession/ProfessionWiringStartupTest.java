package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Proves {@link ProfessionWiring#create} loads bundled profession and recipe resources through the
 * real {@code saveResource} path.
 */
class ProfessionWiringStartupTest {

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void createLoadsBundledProfessionAndRecipeYmlThroughSaveResourcePath() {
    JavaPlugin plugin = MockBukkit.loadSimple(ProfessionWiringTestPlugin.class);
    ProfessionWiring wiring =
        ProfessionWiring.create(plugin, StubJobService.withJobs(bundledJobStorageKeys()));

    assertTrue(Files.isRegularFile(plugin.getDataFolder().toPath().resolve("professions.yml")));
    assertEquals(15, wiring.professionService.tracks().size());
    assertEquals("mining", wiring.professionService.resolve("miner").orElseThrow().id());

    assertNotNull(wiring.recipeService);
    assertTrue(
        wiring
            .recipeService
            .definitionForCraftOutput(Key.key("minecraft", "iron_sword"))
            .isPresent());
    assertTrue(
        wiring
            .recipeService
            .definitionForCraftOutput(Key.key("minecraft", "netherite_pickaxe"))
            .isPresent());
    assertEquals(
        10,
        wiring
            .recipeService
            .definitionForCraftOutput(Key.key("minecraft", "iron_pickaxe"))
            .orElseThrow()
            .requiredLevel());
  }

  private static @NotNull String[] bundledJobStorageKeys() {
    InputStream resource =
        ProfessionWiringTestPlugin.class.getClassLoader().getResourceAsStream("jobs.yml");
    if (resource == null) {
      throw new AssertionError("missing bundled jobs.yml");
    }
    try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
      return yaml.getKeys(false).toArray(String[]::new);
    } catch (IOException exception) {
      throw new AssertionError("failed to read bundled jobs.yml", exception);
    }
  }
}
